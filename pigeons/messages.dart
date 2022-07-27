import 'package:pigeon/pigeon.dart';

class ResponseStarted {
  Map<String?, List<String?>?> headers;
  int statusCode;
  bool isRedirect;
}

class ReadCompleted {
  Uint8List data;
}

enum EventMessageType { responseStarted, readCompleted, tooManyRedirects }

class EventMessage {
  EventMessageType type;
  ResponseStarted? responseStarted;
  ReadCompleted? readCompleted;
}

class StartRequest {
  String url;
  String method;
  Map<String?, String?> headers;
  Uint8List body;
  int maxRedirects;
  bool followRedirects;
}

class StartResponse {
  String eventChannel;
}

@HostApi()
abstract class HttpApi {
  StartResponse start(StartRequest request);
  void dummy(EventMessage message);
}
