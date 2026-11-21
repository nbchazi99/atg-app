import 'dart:async';
import 'dart:js_interop';

import 'package:web/web.dart' as web;

import 'course_asset_upload_result.dart';

Future<CourseAssetUploadResult> uploadCourseAssetInBrowser({
  required Uri endpoint,
  required String token,
  required Uri fileUri,
  required String filename,
}) async {
  final fileResponse = await web.window
      .fetch(fileUri.toString().toJS)
      .toDart
      .timeout(const Duration(minutes: 2));
  if (!fileResponse.ok) {
    throw StateError('The selected browser file could not be opened.');
  }
  final blob = await fileResponse.blob().toDart;
  final form = web.FormData()..append('file', blob, filename);
  final request = web.XMLHttpRequest();
  final completed = Completer<CourseAssetUploadResult>();

  request.open('POST', endpoint.toString());
  request.setRequestHeader('Authorization', 'Bearer $token');
  request.timeout = const Duration(minutes: 15).inMilliseconds;
  request.onload = ((web.Event _) {
    if (!completed.isCompleted) {
      completed.complete(
        CourseAssetUploadResult(
          statusCode: request.status,
          body: request.responseText,
        ),
      );
    }
  }).toJS;
  request.onerror = ((web.Event _) {
    if (!completed.isCompleted) {
      completed.completeError(StateError('The course file upload failed.'));
    }
  }).toJS;
  request.ontimeout = ((web.Event _) {
    if (!completed.isCompleted) {
      completed.completeError(
        TimeoutException('The course file upload timed out.'),
      );
    }
  }).toJS;
  request.send(form);
  return completed.future;
}
