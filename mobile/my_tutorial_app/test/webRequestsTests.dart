import '../lib/net/webRequests.dart';
import 'package:flutter_test/flutter_test.dart';

void main(){
  test('Testing if getWebData returns the correct value', () async {
    final result = await getWebData();
    expect(result, isNotEmpty);
  });

  test('Testing if fetchNumberWordPairs returns the correct value', () async {
    final result = await simpleLongRunningCalculation();
    expect(result.length, equals(100));
  });
}