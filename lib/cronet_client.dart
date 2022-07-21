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

    final bytes = await stream.toBytes();

    final response = await _api.start(StartRequest(
        url: request.url.toString(),
        method: request.method,
        headers: request.headers,
        body: bytes));

    final responseCompleter = Completer<ResponseStarted>();
    final responseDataController = StreamController<Uint8List>();

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
        default:
          throw Exception('Unexpected event');
      }
    }, onDone: () {
      responseDataController.close();
    }, onError: (e) {
      final pe = e as PlatformException;
      final exception = ClientException(pe.message!);
      if (responseCompleter.isCompleted) {
        responseDataController.addError(exception);
      } else {
        responseCompleter.completeError(exception);
      }
      responseDataController.close();
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
