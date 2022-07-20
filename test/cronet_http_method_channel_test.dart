import 'package:flutter2/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:cronet_http/cronet_http_method_channel.dart';

void main() {
  MethodChannelCronetHttp platform = MethodChannelCronetHttp();
  const MethodChannel channel = MethodChannel('cronet_http');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
