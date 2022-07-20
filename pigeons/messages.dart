import 'package:pigeon/pigeon.dart';

class ReadCompleted {
  Uint8List data;
}

class ResponseStarted {
  Map<String?, List<String?>?> headers;
  int statusCode;
  bool isRedirect;
}

class StartRequest {
  String url;
  String method;
  Map<String?, String?> headers;
  Uint8List body;
}

class StartResponse {
  String eventChannel;
}

@HostApi()
abstract class HttpApi {
  StartResponse start(StartRequest request);
  void dummy(ResponseStarted a1, ReadCompleted a2);
}
