import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:cronet_http/cronet_client.dart';
import 'package:http_client_conformance_tests/http_client_conformance_tests.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testAll(CronetClient(), canStreamRequestBody: false);
//  testRequestBody(CronetClient());
}
