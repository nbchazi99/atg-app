import 'api.dart';
import 'realtime.dart';
import 'session.dart';

late Session session;
late Api api;
late Realtime realtime;

/// Uploaded media is stored on the API server; rebase its URL onto whichever
/// server this device is configured to use (localhost in the upload response
/// is not reachable from an emulator or a phone on the LAN).
String mediaUrl(String url) {
  final index = url.indexOf('/uploads/');
  if (index < 0) return url;
  return '${session.server}${url.substring(index + 1)}';
}
