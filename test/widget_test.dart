import 'package:flutter_test/flutter_test.dart';
import 'package:http/testing.dart';
import 'package:my_tutorial_app/net/webRequests.dart';
import 'package:http/http.dart' as http;
import '../my_tutorial_app/flutter/lib/main.dart';

void main() {
  test('Test HTTP requests', () async {
    // Mock HTTP client with a response
    final client = MockClient((request) async {
      if (request.url.host == 'example.com') {
        return http.Response('{"message": "Success"}', 200);
      } else {
        return http.Response('Not Found', 404);
      }
    });

    // Inject the mock client into the webRequests class
    WebRequests.client = client;

    // Call the method and await the result
    await doRequests();

    // Verify the responses
    expect(WebRequests.log, contains('Response status: 200'));
    expect(WebRequests.log, contains('Response body: {"message": "Success"}'));
    expect(WebRequests.log, contains('Response2 status: 404'));
    expect(WebRequests.log, contains('Response2 body: Not Found'));
  });
}

