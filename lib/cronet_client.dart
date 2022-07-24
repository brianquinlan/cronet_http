import 'dart:async';
import 'dart:typed_data';
import 'messages.dart';

import 'package:flutter/services.dart';
import 'package:http/http.dart';

// import 'cronet_http_platform_interface.dart';

late final _api = HttpApi();

class CronetClient extends BaseClient {
  @override
  Future<StreamedResponse> send(BaseRequest request) async {
    final stream = request.finalize();

    final body = await stream.toBytes();

    var headers = request.headers;
    if (body.length > 0 &&
        !headers.keys.any((h) => h.toLowerCase() == 'content-type')) {
      // Cronet requires that requests containing upload data set a
      // 'Content-Type' header.
      headers = Map.from(headers);
      headers['content-type'] = 'application/octet-stream';
    }

    final response = await _api.start(StartRequest(
        url: request.url.toString(),
        method: request.method,
        headers: headers,
        body: body,
        followRedirects: request.followRedirects,
        maxRedirects: request.maxRedirects));

    final responseCompleter = Completer<ResponseStarted>();
    final responseDataController = StreamController<Uint8List>();

    void raiseException(Exception exception) {
      if (responseCompleter.isCompleted) {
        responseDataController.addError(exception);
      } else {
        responseCompleter.completeError(exception);
      }
      responseDataController.close();
    }

    final e = EventChannel(response.eventChannel);
    e.receiveBroadcastStream().listen((event) {
      switch (event[0]) {
        case 0:
          final response = ResponseStarted.decode(event[1]);
          responseCompleter.complete(response);
          break;
        case 1:
          final response = ReadCompleted.decode(event[1]);
          responseDataController.sink.add(response.data);
          break;
        case 2:
          raiseException(
              ClientException('Redirect limit exceeded', request.url));
          break;
        default:
          throw Exception('Unexpected event');
      }
    }, onDone: () {
      responseDataController.close();
    }, onError: (e) {
      final pe = e as PlatformException;
      raiseException(ClientException(pe.message!, request.url));
    });

    final result = await responseCompleter.future;
    final responseHeaders = (result.headers.cast<String, List<Object?>>())
        .map((key, value) => MapEntry(key.toLowerCase(), value.join(",")));

    return StreamedResponse(responseDataController.stream, result.statusCode,
        contentLength: responseHeaders['content-lenght'] as int?,
        isRedirect: result.isRedirect,
        headers: responseHeaders);
  }
}
