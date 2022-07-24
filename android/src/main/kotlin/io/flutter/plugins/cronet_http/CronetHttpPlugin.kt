package io.flutter.plugins.cronet_http;

import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import org.chromium.net.CronetEngine;
import org.chromium.net.UrlRequest;
import org.chromium.net.CronetException
import org.chromium.net.UrlResponseInfo
import org.chromium.net.UploadDataProviders;
import java.nio.ByteBuffer
import android.os.Handler;
import android.os.Looper;

import io.flutter.plugin.common.EventChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

/** CronetHttpPlugin */
class CronetHttpPlugin : FlutterPlugin, Messages.HttpApi {
    private lateinit var flutterPluginBinding: FlutterPlugin.FlutterPluginBinding
    private lateinit var cronetEngine: CronetEngine;
//    private val executor = Executors.newSingleThreadExecutor()
private val executor = Executors.newCachedThreadPool()
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private var channelId = 0

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        this.flutterPluginBinding = flutterPluginBinding;
        val context = flutterPluginBinding.getApplicationContext()
        val builder = CronetEngine.Builder(context)
        cronetEngine = builder.build();
        Messages.HttpApi.setup(flutterPluginBinding.binaryMessenger, this);
    }

    override fun start(startRequest: Messages.StartRequest): Messages.StartResponse {
        val channelName = "plugins.flutter.io/cronet_event/" + channelId++
        val eventChannel =
            EventChannel(flutterPluginBinding.binaryMessenger, channelName);
        lateinit var eventSink: EventChannel.EventSink;
        var numRedirects = 0;

        val cronetRequest = cronetEngine.newUrlRequestBuilder(
            startRequest.url,
            object : UrlRequest.Callback() {
                override fun onRedirectReceived(
                    request: UrlRequest,
                    info: UrlResponseInfo,
                    newLocationUrl: String?
                ) {
                    if (!startRequest.getFollowRedirects()) {
                        request.cancel();
                        mainThreadHandler.post({
                            eventSink.success(
                                listOf(
                                    0, Messages.ResponseStarted.Builder()
                                        .setStatusCode(info.getHttpStatusCode().toLong())
                                        .setHeaders(info.getAllHeaders())
                                        .setIsRedirect(true)
                                        .build().toMap()
                                )
                            )
                        })
                    }
                    ++numRedirects;
                    if (numRedirects <= startRequest.getMaxRedirects()) {
                        request.followRedirect()
                    } else {
                        request.cancel();
                        mainThreadHandler.post({
                            eventSink.success(
                                listOf(
                                    2, Messages.TooManyRedirects.Builder()
                                        .build().toMap()
                                )
                            )
                        })
                    }
                }

                override fun onResponseStarted(request: UrlRequest?, info: UrlResponseInfo) {
                    mainThreadHandler.post({
                        eventSink.success(
                            listOf(
                                0, Messages.ResponseStarted.Builder()
                                    .setStatusCode(info.getHttpStatusCode().toLong())
                                    .setHeaders(info.getAllHeaders())
                                    .setIsRedirect(false)
                                    .build().toMap()
                            )
                        )
                    })
                    request?.read(ByteBuffer.allocateDirect(4096))
                }

                override fun onReadCompleted(
                    request: UrlRequest,
                    info: UrlResponseInfo,
                    byteBuffer: ByteBuffer
                ) {
                    byteBuffer.flip()
                    val b = ByteArray(byteBuffer.remaining())
                    byteBuffer.get(b)
                    mainThreadHandler.post({
                        eventSink.success(
                            listOf(
                                1, Messages.ReadCompleted.Builder()
                                    .setData(b)
                                    .build().toMap()
                            )
                        )
                    })
                    byteBuffer.clear()
                    request?.read(byteBuffer)
                }

                override fun onSucceeded(request: UrlRequest?, info: UrlResponseInfo?) {
                    mainThreadHandler.post({ eventSink.endOfStream() });
                }

                override fun onFailed(
                    request: UrlRequest,
                    info: UrlResponseInfo?,
                    error: CronetException
                ) {
                    mainThreadHandler.post({
                        eventSink.error(
                            "CronetException",
                            error.toString(),
                            ""
                        )
                    });
                }
            },
            executor
        )

        if (startRequest.getBody().size > 0) {
            cronetRequest.setUploadDataProvider(
                UploadDataProviders.create(startRequest.getBody()),
                executor
            )
        }
        cronetRequest.setHttpMethod(startRequest.getMethod());
        for ((key, value) in startRequest.getHeaders()) {
            cronetRequest.addHeader(key, value);
        }

        val streamHandler = object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
                eventSink = events
                try {
                    cronetRequest.build().start()
                } catch (e: Exception) {
                    mainThreadHandler.post({
                        eventSink.error(
                            "CronetException",
                            e.toString(),
                            ""
                        )
                    });
                }
            }

            override fun onCancel(arguments: Any?) {
            }
        }
        eventChannel.setStreamHandler(streamHandler);

        return Messages.StartResponse.Builder()
            .setEventChannel(channelName)
            .build()
    }


    override fun dummy(arg1: Messages.ResponseStarted, a2: Messages.ReadCompleted, a3: Messages.TooManyRedirects) {
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        Messages.HttpApi.setup(binding.binaryMessenger, null)
    }
}
