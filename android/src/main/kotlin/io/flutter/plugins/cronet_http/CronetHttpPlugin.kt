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
import java.nio.ByteBuffer
import android.os.Handler;
import android.os.Looper;

import io.flutter.plugin.common.EventChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

/** CronetHttpPlugin */
class CronetHttpPlugin : FlutterPlugin, Messages.HttpApi {
    lateinit var flutterPluginBinding: FlutterPlugin.FlutterPluginBinding

    /*
    class Foo : EventChannel.StreamHandler {
        public lateinit var eventSink: EventChannel.EventSink;
        override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
            eventSink = events
//            eventSink.success(byteArrayOf(72, 101, 108, 108, 111, 32, 119, 111, 114, 108, 100));
//            eventSink.endOfStream();
        }

        override fun onCancel(arguments: Any?) {
        }

        public fun dart() {

        }
    }
    */

    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private lateinit var cronetEngine: CronetEngine;
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val mainThreadHandler: Handler = Handler(Looper.getMainLooper())

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        this.flutterPluginBinding = flutterPluginBinding;
        val context = flutterPluginBinding.getApplicationContext()
        val builder = CronetEngine.Builder(context)
        cronetEngine = builder.build();
        Messages.HttpApi.setup(flutterPluginBinding.binaryMessenger, this);
    }

    override fun start(request: Messages.StartRequest): Messages.StartResponse {
        val eventChannel =
            EventChannel(flutterPluginBinding.binaryMessenger, "plugins.flutter.io/cronet_event");
        lateinit var eventSink: EventChannel.EventSink;

        val request = cronetEngine.newUrlRequestBuilder(
            request.url,
            object : UrlRequest.Callback() {
                override fun onRedirectReceived(
                    request: UrlRequest?,
                    info: UrlResponseInfo?,
                    newLocationUrl: String?
                ) {
                    // You should call the request.followRedirect() method to continue
                    // processing the request.
                    request?.followRedirect()
                }

                override fun onResponseStarted(request: UrlRequest?, info: UrlResponseInfo) {
                    mainThreadHandler.post({eventSink.success(
                        listOf(
                            0, Messages.ResponseStarted.Builder()
                                .setStatusCode(info.getHttpStatusCode().toLong())
                                .setHeaders(info.getAllHeaders())
                                .setIsRedirect(false)
                                .build().toMap()
                        )
                    )})
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
                    mainThreadHandler.post({eventSink.success(
                        listOf(
                            1, Messages.ReadCompleted.Builder()
                                .setData(b)
                                .build().toMap()
                        )
                    )})
                    byteBuffer.clear()
                    request?.read(byteBuffer)
                }

                override fun onSucceeded(request: UrlRequest?, info: UrlResponseInfo?) {
                    mainThreadHandler.post({eventSink.endOfStream()});
                }

                override fun onFailed(
                    request: UrlRequest,
                    info: UrlResponseInfo?,
                    error: CronetException
                ) {
                    mainThreadHandler.post({eventSink.error(
                        "CronetException",
                        error.toString(),
                        ""
                    )});
                }
            },
            executor
        ).build()

        val foo = object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
                eventSink = events
                request.start()
            }

            override fun onCancel(arguments: Any?) {
            }
        }
        eventChannel.setStreamHandler(foo);

        return Messages.StartResponse.Builder()
            .setEventChannel("plugins.flutter.io/cronet_event")
            .build()
    }


    override fun dummy(arg1: Messages.ResponseStarted, a2: Messages.ReadCompleted) {
    }


    /*

        responseController.stream, response['statusCode'] as int,
      contentLength: response['contentLength'] as int?,
      isRedirect: response['isRedirect'] as bool,≈≈≈∂
      headers: (response['headers'] as Map<String, String>)

*/
    //     final r = await methodChannel.invokeMethod<Map<String, Object>>(
    //   'startRequest', [url.toString(), method, headers, bytes]);
/*
  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "startRequest") {
      val url: String? = call.argument("url");
      if (url == null) {
        result.error("startRequest error", "url cannot be null")
      }

      val requestBuilder = cronetEngine.newUrlRequestBuilder(
        url,
        MyUrlRequestCallback(),
        executor)
)

val request: UrlRequest = requestBuilder.build()

      result.success(mapOf("statusCode" to 200, "contentLength" to 200, "isRedirect" to false, "headers" to emptyMap<String, String>()));
    } else {
      result.notImplemented()
    }
  }
*/
    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        Messages.HttpApi.setup(binding.binaryMessenger, null)
    }
}
