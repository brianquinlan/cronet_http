import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'cronet_http_method_channel.dart';

abstract class CronetHttpPlatform extends PlatformInterface {
  /// Constructs a CronetHttpPlatform.
  CronetHttpPlatform() : super(token: _token);

  static final Object _token = Object();

  static CronetHttpPlatform _instance = MethodChannelCronetHttp();

  /// The default instance of [CronetHttpPlatform] to use.
  ///
  /// Defaults to [MethodChannelCronetHttp].
  static CronetHttpPlatform get instance => _instance;
  
  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [CronetHttpPlatform] when
  /// they register themselves.
  static set instance(CronetHttpPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
