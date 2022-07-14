
import 'cronet_http_platform_interface.dart';

class CronetHttp {
  Future<String?> getPlatformVersion() {
    return CronetHttpPlatform.instance.getPlatformVersion();
  }
}
