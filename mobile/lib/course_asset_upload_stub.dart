import 'course_asset_upload_result.dart';

Future<CourseAssetUploadResult> uploadCourseAssetInBrowser({
  required Uri endpoint,
  required String token,
  required Uri fileUri,
  required String filename,
}) {
  throw UnsupportedError('Browser file uploads are not available here.');
}
