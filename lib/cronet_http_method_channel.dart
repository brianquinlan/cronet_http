import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'cronet_http_platform_interface.dart';

/// An implementation of [CronetHttpPlatform] that uses method channels.
class MethodChannelCronetHttp extends CronetHttpPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('cronet_http');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
