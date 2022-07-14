import 'package:flutter_test/flutter_test.dart';
import 'package:cronet_http/cronet_http.dart';
import 'package:cronet_http/cronet_http_platform_interface.dart';
import 'package:cronet_http/cronet_http_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockCronetHttpPlatform 
    with MockPlatformInterfaceMixin
    implements CronetHttpPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final CronetHttpPlatform initialPlatform = CronetHttpPlatform.instance;

  test('$MethodChannelCronetHttp is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelCronetHttp>());
  });

  test('getPlatformVersion', () async {
    CronetHttp cronetHttpPlugin = CronetHttp();
    MockCronetHttpPlatform fakePlatform = MockCronetHttpPlatform();
    CronetHttpPlatform.instance = fakePlatform;
  
    expect(await cronetHttpPlugin.getPlatformVersion(), '42');
  });
}
