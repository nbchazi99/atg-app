import 'dart:async';
import 'dart:convert';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';
import 'package:image_picker/image_picker.dart';

import 'course_asset_upload_stub.dart'
    if (dart.library.html) 'course_asset_upload_web.dart'
    as browser_upload;
import 'models.dart';
import 'session.dart';

const connectionMessage =
    'Cannot reach the ATG server. Check the server address and that the API is running.';

class ApiException implements Exception {
  const ApiException(this.message, [this.status]);

  final String message;
  final int? status;

  @override
  String toString() => message;
}

class Api {
  Api(this.session);

  static const _maxCourseAssetBytes = 250 * 1024 * 1024;
  static const _maxCourseCoverBytes = 10 * 1024 * 1024;
  static const _courseCoverExtensions = {'.jpg', '.jpeg', '.png', '.webp'};
  static const _maxChatAttachmentBytes = 80 * 1024 * 1024;
  static const _courseAssetMimeTypes = <String, String>{
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.png': 'image/png',
    '.webp': 'image/webp',
    '.gif': 'image/gif',
    '.mp4': 'video/mp4',
    '.webm': 'video/webm',
    '.mov': 'video/quicktime',
    '.m4v': 'video/mp4',
    '.pdf': 'application/pdf',
    '.doc': 'application/msword',
    '.docx': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    '.xls': 'application/vnd.ms-excel',
    '.xlsx':
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    '.ppt': 'application/vnd.ms-powerpoint',
    '.pptx': 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    '.mp3': 'audio/mpeg',
    '.wav': 'audio/wav',
    '.m4a': 'audio/mp4',
    '.aac': 'audio/aac',
    '.ogg': 'audio/ogg',
    '.opus': 'audio/ogg',
    '.flac': 'audio/flac',
  };

  final Session session;
  final http.Client _client = http.Client();

  List<T> _parseList<T>(dynamic data, T Function(Map<String, dynamic>) parse) {
    if (data is! List) return <T>[];
    return data.map((e) => parse((e as Map).cast<String, dynamic>())).toList();
  }

  Future<dynamic> _send(
    String method,
    String path, {
    Object? body,
    Map<String, String>? query,
  }) async {
    var uri = Uri.parse('${session.server}$path');
    if (query != null && query.isNotEmpty) {
      uri = uri.replace(queryParameters: query);
    }
    final request = http.Request(method, uri);
    final token = session.token;
    if (token != null && token.isNotEmpty) {
      request.headers['Authorization'] = 'Bearer $token';
    }
    if (body != null) {
      request.headers['Content-Type'] = 'application/json';
      request.body = jsonEncode(body);
    }

    http.Response response;
    try {
      // 90s: Render free tier needs ~50s to wake a spun-down instance.
      final streamed = await _client
          .send(request)
          .timeout(const Duration(seconds: 90));
      response = await http.Response.fromStream(streamed);
    } on ApiException {
      rethrow;
    } catch (error) {
      debugPrint('ATG request failed: $uri -> $error');
      throw const ApiException(connectionMessage);
    }

    dynamic decoded;
    if (response.body.isNotEmpty) {
      try {
        decoded = jsonDecode(response.body);
      } catch (_) {
        decoded = null;
      }
    }

    if (response.statusCode >= 200 && response.statusCode < 300) return decoded;

    if (response.statusCode == 401) {
      final message = method == 'POST' && path == 'api/auth/login'
          ? 'Email or password is incorrect.'
          : 'Your session ended. Sign in again.';
      throw ApiException(message, 401);
    }
    final message = decoded is Map && decoded['error'] is String
        ? decoded['error'] as String
        : 'Request failed (${response.statusCode})';
    throw ApiException(message, response.statusCode);
  }

  Future<AuthResult> login(String email, String password) async {
    final data = await _send(
      'POST',
      'api/auth/login',
      body: {'email': email, 'password': password},
    );
    return AuthResult.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<AuthResult> register(
    String name,
    String email,
    String phone,
    String password,
  ) async {
    final data = await _send(
      'POST',
      'api/auth/register',
      body: {
        'name': name,
        'email': email,
        'phone': phone,
        'password': password,
      },
    );
    return AuthResult.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<void> requestPasswordReset(String email) async {
    await _send(
      'POST',
      'api/auth/password-reset/request',
      body: {'email': email.trim()},
    );
  }

  Future<void> resetPassword({
    required String email,
    required String code,
    required String password,
  }) async {
    await _send(
      'POST',
      'api/auth/password-reset/confirm',
      body: {'email': email.trim(), 'code': code, 'password': password},
    );
  }

  Future<AppUser> me() async {
    final data = await _send('GET', 'api/me');
    return AppUser.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<AuthResult> updateProfile({
    String? name,
    String? username,
    String? phone,
    String? country,
    String? email,
    String? currentPassword,
  }) async {
    final body = <String, dynamic>{};
    if (name != null) body['name'] = name;
    if (username != null) body['username'] = username;
    if (phone != null) body['phone'] = phone;
    if (country != null) body['country'] = country;
    if (email != null) body['email'] = email;
    if (currentPassword != null) body['currentPassword'] = currentPassword;
    final data = await _send('PUT', 'api/me', body: body);
    return AuthResult.fromProfileUpdateJson(
      (data as Map).cast<String, dynamic>(),
    );
  }

  Future<AppUser> removeProfileAvatar() async {
    final data = await _send('DELETE', 'api/me/avatar');
    return AppUser.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<AppUser> uploadProfileAvatar(XFile file) async {
    final size = await file.length();
    final extension = _extension(file.name);
    if (!_courseCoverExtensions.contains(extension)) {
      throw const ApiException('Choose a JPG, PNG, or WebP profile photo.');
    }
    if (size <= 0) {
      throw const ApiException('The selected profile photo is empty.');
    }
    if (size > 5 * 1024 * 1024) {
      throw const ApiException('Profile photos must be 5 MB or smaller.');
    }
    final mime = _courseAssetMimeTypes[extension]!;
    final endpoint = Uri.parse('${session.server}api/me/avatar');
    final token = session.token;
    if (token == null || token.isEmpty) {
      throw const ApiException('Sign in to update your profile photo.');
    }
    try {
      if (kIsWeb) {
        final response = await browser_upload.uploadCourseAssetInBrowser(
          endpoint: endpoint,
          token: token,
          fileUri: Uri.parse(file.path),
          filename: file.name,
        );
        return _uploadedProfileAvatar(response.statusCode, response.body);
      }
      final request = http.MultipartRequest('POST', endpoint)
        ..headers['Authorization'] = 'Bearer $token'
        ..files.add(
          http.MultipartFile(
            'file',
            file.openRead(),
            size,
            filename: file.name,
            contentType: MediaType.parse(mime),
          ),
        );
      final streamed = await request.send().timeout(const Duration(minutes: 5));
      final response = await http.Response.fromStream(streamed);
      return _uploadedProfileAvatar(response.statusCode, response.body);
    } on ApiException {
      rethrow;
    } on TimeoutException {
      throw const ApiException(
        'The profile photo upload timed out. Check your connection and try again.',
      );
    } catch (error, stackTrace) {
      debugPrint('ATG profile photo upload failed: $error\n$stackTrace');
      throw const ApiException(
        'Could not upload the profile photo. Check your connection and try again.',
      );
    }
  }

  AppUser _uploadedProfileAvatar(int statusCode, String body) {
    dynamic decoded;
    try {
      decoded = body.isEmpty ? null : jsonDecode(body);
    } catch (_) {
      decoded = null;
    }
    if (statusCode >= 200 && statusCode < 300 && decoded is Map) {
      return AppUser.fromJson(decoded.cast<String, dynamic>());
    }
    final message = decoded is Map && decoded['message'] is String
        ? decoded['message'] as String
        : decoded is Map && decoded['error'] is String
        ? decoded['error'] as String
        : 'Could not upload the profile photo ($statusCode)';
    throw ApiException(message, statusCode);
  }

  Future<HomeData> home() async {
    final data = await _send('GET', 'api/home');
    return HomeData.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<Settings> settings() async {
    final data = await _send('GET', 'api/settings/public');
    return Settings.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<List<Signal>> signals({String? status, String? symbol}) async {
    final data = await _send(
      'GET',
      'api/signals',
      query: {
        if (status != null && status.isNotEmpty) 'status': status,
        if (symbol != null && symbol.isNotEmpty) 'symbol': symbol,
      },
    );
    return _parseList(data, Signal.fromJson);
  }

  Future<List<Room>> rooms() async {
    final data = await _send('GET', 'api/rooms');
    return _parseList(data, Room.fromJson);
  }

  Future<Room> createRoom({
    required String name,
    required String description,
  }) async {
    final data = await _send(
      'POST',
      'api/admin/rooms',
      body: {'name': name, 'description': description},
    );
    return Room.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<List<FeedItem>> feed(String filter) async {
    final data = await _send(
      'GET',
      'api/rooms/feed',
      query: {'filter': filter},
    );
    return _parseList(data, FeedItem.fromJson);
  }

  Future<FeedItem> createThread({
    required String title,
    required String message,
  }) async {
    final data = await _send(
      'POST',
      'api/rooms/community/threads',
      body: {'title': title, 'message': message},
    );
    return FeedItem.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<FeedItem> setThreadOpen({
    required int roomId,
    required int threadId,
    required bool open,
  }) async {
    final data = await _send(
      'PATCH',
      'api/rooms/$roomId/threads/$threadId',
      body: {'open': open},
    );
    return FeedItem.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<List<RoomMessage>> messages(int roomId, {int? threadId}) async {
    final data = await _send(
      'GET',
      'api/rooms/$roomId/messages',
      query: threadId == null ? null : {'threadId': '$threadId'},
    );
    return _parseList(data, RoomMessage.fromJson);
  }

  Future<RoomMessage> sendMessage(
    int roomId,
    String text, {
    int? threadId,
    String? attachmentUrl,
    String? attachmentType,
  }) async {
    final data = await _send(
      'POST',
      'api/rooms/$roomId/messages',
      query: threadId == null ? null : {'threadId': '$threadId'},
      body: {
        'text': text,
        'attachmentUrl': ?attachmentUrl,
        'attachmentType': ?attachmentType,
      },
    );
    return RoomMessage.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<void> deleteRoomMessage(int roomId, int messageId) async {
    await _send('DELETE', 'api/admin/rooms/$roomId/messages/$messageId');
  }

  Future<WalletData> wallet() async {
    final data = await _send('GET', 'api/wallet');
    return WalletData.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<DepositResult> deposit({
    required String broker,
    required String clientId,
    required double amount,
    required String wallet,
    required String mobile,
  }) async {
    final data = await _send(
      'POST',
      'api/deposits',
      body: {
        'broker': broker,
        'clientId': clientId,
        'amount': amount,
        'wallet': wallet,
        'mobile': mobile,
      },
    );
    return DepositResult.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<WithdrawResult> withdraw({
    required String payout,
    required String mobile,
    required double amount,
    required String sourceType,
    required String sourceDetail,
    required String proofUrl,
  }) async {
    final data = await _send(
      'POST',
      'api/withdrawals',
      body: {
        'payout': payout,
        'mobile': mobile,
        'amount': amount,
        'sourceType': sourceType,
        'sourceDetail': sourceDetail,
        'proofUrl': proofUrl,
      },
    );
    return WithdrawResult.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<String> uploadImage(XFile file) async {
    return _uploadMedia(file, video: false);
  }

  Future<String> uploadVideo(XFile file) async {
    return _uploadMedia(file, video: true);
  }

  Future<String> uploadRoomAttachment(int roomId, PlatformFile file) async {
    final size = await file.length();
    if (size == null) {
      throw const ApiException(
        'Could not determine the selected file size. Please select it again.',
      );
    }
    _validateChatAttachment(file.name, size);
    final endpoint = Uri.parse(
      '${session.server}api/rooms/$roomId/attachments',
    );
    if (kIsWeb) {
      return _uploadChatAttachmentInBrowser(endpoint, file.uri, file.name);
    }
    return _uploadChatAttachmentStream(
      endpoint,
      file.name,
      size,
      file.readAsByteStream(),
    );
  }

  Future<String> uploadRoomRecording(int roomId, XFile file) async {
    final size = await file.length();
    _validateChatAttachment(file.name, size);
    final endpoint = Uri.parse(
      '${session.server}api/rooms/$roomId/attachments',
    );
    if (kIsWeb) {
      return _uploadChatAttachmentInBrowser(
        endpoint,
        Uri.parse(file.path),
        file.name,
      );
    }
    return _uploadChatAttachmentStream(
      endpoint,
      file.name,
      size,
      file.openRead(),
    );
  }

  void _validateChatAttachment(String filename, int size) {
    if (!_courseAssetMimeTypes.containsKey(_extension(filename))) {
      throw const ApiException(
        'Choose an image, video, PDF, Office document, or audio file.',
      );
    }
    if (size <= 0) {
      throw const ApiException('The selected file is empty.');
    }
    if (size > _maxChatAttachmentBytes) {
      throw const ApiException('Chat files must be 80 MB or smaller.');
    }
  }

  String _extension(String filename) {
    final lower = filename.toLowerCase();
    final dot = lower.lastIndexOf('.');
    return dot < 0 ? '' : lower.substring(dot);
  }

  Future<String> _uploadChatAttachmentStream(
    Uri endpoint,
    String filename,
    int size,
    Stream<List<int>> stream,
  ) async {
    final token = session.token;
    if (token == null || token.isEmpty) {
      throw const ApiException('Sign in to attach files to chat.');
    }
    final request = http.MultipartRequest('POST', endpoint)
      ..headers['Authorization'] = 'Bearer $token';
    try {
      request.files.add(
        http.MultipartFile(
          'file',
          stream,
          size,
          filename: filename,
          contentType: MediaType.parse(
            _courseAssetMimeTypes[_extension(filename)]!,
          ),
        ),
      );
      final streamed = await request.send().timeout(
        const Duration(minutes: 15),
      );
      final response = await http.Response.fromStream(streamed);
      return _uploadedChatAttachmentUrl(response.statusCode, response.body);
    } on ApiException {
      rethrow;
    } on TimeoutException {
      throw const ApiException(
        'The upload timed out. Check your connection and try again.',
      );
    } catch (error, stackTrace) {
      debugPrint('ATG chat-file upload failed: $error\n$stackTrace');
      throw const ApiException(
        'Could not upload the chat file. Check your connection and try again.',
      );
    }
  }

  Future<String> _uploadChatAttachmentInBrowser(
    Uri endpoint,
    Uri fileUri,
    String filename,
  ) async {
    final token = session.token;
    if (token == null || token.isEmpty) {
      throw const ApiException('Sign in to attach files to chat.');
    }
    try {
      final response = await browser_upload.uploadCourseAssetInBrowser(
        endpoint: endpoint,
        token: token,
        fileUri: fileUri,
        filename: filename,
      );
      return _uploadedChatAttachmentUrl(response.statusCode, response.body);
    } on ApiException {
      rethrow;
    } on TimeoutException {
      throw const ApiException(
        'The upload timed out. Check your connection and try again.',
      );
    } catch (error, stackTrace) {
      debugPrint('ATG browser chat-file upload failed: $error\n$stackTrace');
      throw const ApiException(
        'Could not upload the chat file. Check your connection and try again.',
      );
    }
  }

  String _uploadedChatAttachmentUrl(int statusCode, String body) {
    dynamic decoded;
    try {
      decoded = body.isEmpty ? null : jsonDecode(body);
    } catch (_) {
      decoded = null;
    }
    if (statusCode >= 200 &&
        statusCode < 300 &&
        decoded is Map &&
        decoded['url'] is String) {
      return decoded['url'] as String;
    }
    final message = decoded is Map && decoded['message'] is String
        ? decoded['message'] as String
        : decoded is Map && decoded['error'] is String
        ? decoded['error'] as String
        : 'Could not upload the chat file ($statusCode)';
    throw ApiException(message, statusCode);
  }

  Future<String> uploadCourseAsset(XFile file) async {
    final size = await file.length();
    if (kIsWeb) {
      return _uploadCourseAssetInBrowser(Uri.parse(file.path), file.name, size);
    }
    return _uploadCourseAssetStream(file.name, size, file.openRead());
  }

  Future<String> uploadCourseCover(XFile file) async {
    final size = await file.length();
    final lower = file.name.toLowerCase();
    final dot = lower.lastIndexOf('.');
    final extension = dot < 0 ? '' : lower.substring(dot);
    if (!_courseCoverExtensions.contains(extension)) {
      throw const ApiException('Choose a JPG, PNG, or WebP course cover.');
    }
    _validateCourseAssetSize(size, maxBytes: _maxCourseCoverBytes);
    if (kIsWeb) {
      return _uploadCourseAssetInBrowser(
        Uri.parse(file.path),
        file.name,
        size,
        endpoint: 'api/admin/course-covers',
        maxBytes: _maxCourseCoverBytes,
      );
    }
    return _uploadCourseAssetStream(
      file.name,
      size,
      file.openRead(),
      endpoint: 'api/admin/course-covers',
      maxBytes: _maxCourseCoverBytes,
    );
  }

  Future<String> uploadCourseMaterial(PlatformFile file) async {
    final size = await file.length();
    if (size == null) {
      throw const ApiException(
        'Could not determine the selected file size. Please select it again.',
      );
    }
    if (kIsWeb) {
      return _uploadCourseAssetInBrowser(file.uri, file.name, size);
    }
    return _uploadCourseAssetStream(file.name, size, file.readAsByteStream());
  }

  Future<String> _uploadCourseAssetStream(
    String filename,
    int size,
    Stream<List<int>> stream, {
    String endpoint = 'api/admin/course-assets',
    int maxBytes = _maxCourseAssetBytes,
  }) async {
    final mime = _courseAssetMimeType(filename);
    _validateCourseAssetSize(size, maxBytes: maxBytes);

    final uri = Uri.parse('${session.server}$endpoint');
    final request = http.MultipartRequest('POST', uri);
    final token = session.token;
    if (token != null && token.isNotEmpty) {
      request.headers['Authorization'] = 'Bearer $token';
    }

    try {
      request.files.add(
        http.MultipartFile(
          'file',
          stream,
          size,
          filename: filename,
          contentType: MediaType.parse(mime),
        ),
      );
      final streamed = await request.send().timeout(
        const Duration(minutes: 15),
      );
      final response = await http.Response.fromStream(streamed);
      return _uploadedCourseAssetUrl(response.statusCode, response.body);
    } on ApiException {
      rethrow;
    } on TimeoutException {
      throw const ApiException(
        'The upload timed out. Check your connection and try again.',
      );
    } catch (error, stackTrace) {
      debugPrint('ATG course-file upload failed: $error\n$stackTrace');
      throw const ApiException(
        'Could not upload the course file. Check your connection and try again.',
      );
    }
  }

  Future<String> _uploadCourseAssetInBrowser(
    Uri fileUri,
    String filename,
    int size, {
    String endpoint = 'api/admin/course-assets',
    int maxBytes = _maxCourseAssetBytes,
  }) async {
    _courseAssetMimeType(filename);
    _validateCourseAssetSize(size, maxBytes: maxBytes);
    final token = session.token;
    if (token == null || token.isEmpty) {
      throw const ApiException('Sign in as an admin to upload course files.');
    }
    try {
      final response = await browser_upload.uploadCourseAssetInBrowser(
        endpoint: Uri.parse('${session.server}$endpoint'),
        token: token,
        fileUri: fileUri,
        filename: filename,
      );
      return _uploadedCourseAssetUrl(response.statusCode, response.body);
    } on ApiException {
      rethrow;
    } on TimeoutException {
      throw const ApiException(
        'The upload timed out. Check your connection and try again.',
      );
    } catch (error, stackTrace) {
      debugPrint('ATG browser course-file upload failed: $error\n$stackTrace');
      throw const ApiException(
        'Could not upload the course file. Check your connection and try again.',
      );
    }
  }

  String _courseAssetMimeType(String filename) {
    final lower = filename.toLowerCase();
    final dot = lower.lastIndexOf('.');
    final extension = dot < 0 ? '' : lower.substring(dot);
    final mime = _courseAssetMimeTypes[extension];
    if (mime == null) {
      throw const ApiException(
        'Choose an image, video, PDF, Office document, or audio file.',
      );
    }
    return mime;
  }

  void _validateCourseAssetSize(
    int size, {
    int maxBytes = _maxCourseAssetBytes,
  }) {
    if (size <= 0) {
      throw const ApiException('The selected file is empty.');
    }
    if (size > maxBytes) {
      throw ApiException(
        maxBytes == _maxCourseCoverBytes
            ? 'Course cover images must be 10 MB or smaller.'
            : 'Course files must be 250 MB or smaller.',
      );
    }
  }

  String _uploadedCourseAssetUrl(int statusCode, String body) {
    dynamic decoded;
    try {
      decoded = body.isEmpty ? null : jsonDecode(body);
    } catch (_) {
      decoded = null;
    }
    if (statusCode >= 200 &&
        statusCode < 300 &&
        decoded is Map &&
        decoded['url'] is String) {
      return decoded['url'] as String;
    }
    final message = decoded is Map && decoded['message'] is String
        ? decoded['message'] as String
        : decoded is Map && decoded['error'] is String
        ? decoded['error'] as String
        : 'Could not upload the course file ($statusCode)';
    throw ApiException(message, statusCode);
  }

  Future<String> _uploadMedia(XFile file, {required bool video}) async {
    final uri = Uri.parse('${session.server}api/uploads');
    final request = http.MultipartRequest('POST', uri);
    final token = session.token;
    if (token != null && token.isNotEmpty) {
      request.headers['Authorization'] = 'Bearer $token';
    }

    final name = file.name.isEmpty
        ? (video ? 'lesson.mp4' : 'proof.jpg')
        : file.name;
    final lower = name.toLowerCase();
    final mime = video
        ? lower.endsWith('.webm')
              ? 'video/webm'
              : lower.endsWith('.mov')
              ? 'video/quicktime'
              : lower.endsWith('.m4v')
              ? 'video/x-m4v'
              : 'video/mp4'
        : lower.endsWith('.png')
        ? 'image/png'
        : lower.endsWith('.webp')
        ? 'image/webp'
        : lower.endsWith('.heic')
        ? 'image/heic'
        : 'image/jpeg';

    try {
      request.files.add(
        http.MultipartFile.fromBytes(
          'file',
          await file.readAsBytes(),
          filename: name,
          contentType: MediaType.parse(mime),
        ),
      );
      final streamed = await request.send().timeout(
        const Duration(seconds: 90),
      );
      final response = await http.Response.fromStream(streamed);
      dynamic decoded;
      try {
        decoded = response.body.isEmpty ? null : jsonDecode(response.body);
      } catch (_) {
        decoded = null;
      }
      if (response.statusCode >= 200 &&
          response.statusCode < 300 &&
          decoded is Map &&
          decoded['url'] is String) {
        return decoded['url'] as String;
      }
      final message = decoded is Map && decoded['error'] is String
          ? decoded['error'] as String
          : 'Could not upload that ${video ? 'video' : 'image'} (${response.statusCode})';
      throw ApiException(message, response.statusCode);
    } on ApiException {
      rethrow;
    } catch (_) {
      throw const ApiException(connectionMessage);
    }
  }

  Future<List<CourseRow>> courses() async {
    final data = await _send('GET', 'api/courses');
    return _parseList(data, CourseRow.fromJson);
  }

  Future<CourseDetail> course(int id) async {
    final data = await _send('GET', 'api/courses/$id');
    return CourseDetail.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<void> completeLesson(int courseId, int lessonId) async {
    await _send(
      'POST',
      'api/courses/$courseId/complete',
      body: {'lessonId': lessonId},
    );
  }

  Future<List<AcademyComment>> academyComments(
    int courseId, {
    int? lessonId,
  }) async {
    final path = lessonId == null
        ? 'api/courses/$courseId/comments'
        : 'api/courses/$courseId/lessons/$lessonId/comments';
    final data = await _send('GET', path);
    return _parseList(data, AcademyComment.fromJson);
  }

  Future<AcademyComment> addAcademyComment(
    int courseId,
    String text, {
    int? lessonId,
  }) async {
    final path = lessonId == null
        ? 'api/courses/$courseId/comments'
        : 'api/courses/$courseId/lessons/$lessonId/comments';
    final data = await _send('POST', path, body: {'text': text});
    return AcademyComment.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<void> enrollInFreeCourse(int courseId) async {
    await _send('POST', 'api/courses/$courseId/enroll');
  }

  Future<List<Purchase>> purchases() async {
    final data = await _send('GET', 'api/purchases');
    return _parseList(data, Purchase.fromJson);
  }

  Future<Purchase> purchase(
    String type, {
    int? courseId,
    String? proofUrl,
  }) async {
    final data = await _send(
      'POST',
      'api/purchases',
      body: {'type': type, 'courseId': ?courseId, 'proofUrl': ?proofUrl},
    );
    return Purchase.fromJson((data as Map).cast<String, dynamic>());
  }

  /// Creates a gateway order; an EcoCash payment prompt is pushed to the
  /// customer's phone ([ecocashStatus] is Dischub's prompt state).
  Future<({PaymentTx order, String ecocashStatus})> createPaymentIntent({
    required String type,
    int? courseId,
    required String senderPhone,
  }) async {
    final data = await _send(
      'POST',
      'api/payments/intents',
      body: {'type': type, 'courseId': ?courseId, 'senderPhone': senderPhone},
    );
    final map = (data as Map).cast<String, dynamic>();
    return (
      order: PaymentTx.fromJson((map['order'] as Map).cast<String, dynamic>()),
      ecocashStatus: map['ecocashStatus'] == null
          ? ''
          : '${map['ecocashStatus']}',
    );
  }

  /// Server-side verified status; the backend re-queries the gateway on read.
  Future<PaymentTx> paymentStatus(String orderId) async {
    final data = await _send('GET', 'api/payments/$orderId');
    return PaymentTx.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<List<PaymentTx>> paymentHistory() async {
    final data = await _send('GET', 'api/payments/history');
    return _parseList(data, PaymentTx.fromJson);
  }

  Future<List<PaymentTx>> adminPayments({String? status}) async {
    final data = await _send(
      'GET',
      'api/admin/payments',
      query: {if (status != null && status.isNotEmpty) 'status': status},
    );
    return _parseList(data, PaymentTx.fromJson);
  }

  Future<PaymentTx> refundPayment(String orderId, String note) async {
    final data = await _send(
      'POST',
      'api/admin/payments/$orderId/refund',
      body: {'note': note},
    );
    return PaymentTx.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<List<AppUser>> adminUsers() async {
    final data = await _send('GET', 'api/admin/users');
    return _parseList(data, AppUser.fromJson);
  }

  Future<AppUser> setMembership(int userId, String membership) async {
    final data = await _send(
      'PATCH',
      'api/admin/users/$userId/membership',
      body: {'membership': membership},
    );
    return AppUser.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<AppUser> assignAdmin(int userId) async {
    final data = await _send('POST', 'api/admin/users/$userId/assign-admin');
    return AppUser.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<AdminRequests> adminRequests() async {
    final data = await _send('GET', 'api/admin/requests');
    return AdminRequests.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<void> decide(String kind, int id, bool approve, String note) async {
    final path = kind == 'DEPOSIT'
        ? 'api/admin/deposits/$id/decide'
        : kind == 'WITHDRAWAL'
        ? 'api/admin/withdrawals/$id/decide'
        : 'api/admin/purchases/$id/decide';
    await _send('POST', path, body: {'approve': approve, 'note': note});
  }

  Future<List<Signal>> adminSignals() async {
    final data = await _send('GET', 'api/admin/signals');
    return _parseList(data, Signal.fromJson);
  }

  Future<Signal> createSignal(Signal signal) async {
    final data = await _send(
      'POST',
      'api/admin/signals',
      body: _signalBody(signal),
    );
    return Signal.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<Signal> updateSignal(Signal signal) async {
    final data = await _send(
      'PUT',
      'api/admin/signals/${signal.id}',
      body: _signalBody(signal),
    );
    return Signal.fromJson((data as Map).cast<String, dynamic>());
  }

  Map<String, Object?> _signalBody(Signal signal) => {
    'title': signal.title,
    'subtitle': signal.subtitle,
    'symbol': signal.symbol,
    'side': signal.side,
    'status': signal.status,
    'roomLabel': signal.roomLabel,
    'entry': signal.entry,
    'stopLoss': signal.stopLoss,
    'targets': signal.targets,
    'ruleName': signal.ruleName,
    'participants': signal.participants,
    'result': signal.result,
    'closingPrice': signal.closingPrice,
    'closingUpdate': signal.closingUpdate,
  };

  Future<void> deleteSignal(int id) async {
    await _send('DELETE', 'api/admin/signals/$id');
  }

  Future<Course> createCourse({
    required String title,
    required String subtitle,
    required String description,
    required double price,
    required String level,
    String imageUrl = '',
    required List<Map<String, Object?>> lessons,
  }) async {
    final data = await _send(
      'POST',
      'api/admin/courses',
      body: {
        'title': title,
        'subtitle': subtitle,
        'description': description,
        'price': price,
        'level': level,
        'imageUrl': imageUrl,
        'lessons': lessons,
      },
    );
    return Course.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<List<Course>> adminCourses() async {
    final data = await _send('GET', 'api/admin/courses');
    return _parseList(data, Course.fromJson);
  }

  Future<CourseDetail> adminCourseDetail(int id) async {
    final data = await _send('GET', 'api/admin/courses/$id');
    return CourseDetail.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<Course> reviewCourse(
    int id, {
    required bool approve,
    String? note,
  }) async {
    final data = await _send(
      'POST',
      'api/admin/courses/$id/review',
      body: {'approve': approve, 'note': note},
    );
    return Course.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<void> deleteCourse(int id) async {
    await _send('DELETE', 'api/admin/courses/$id');
  }

  Future<Settings> adminSettings() async {
    final data = await _send('GET', 'api/admin/settings');
    return Settings.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<Settings> updateSettings(Map<String, Object?> values) async {
    final data = await _send('PUT', 'api/admin/settings', body: values);
    return Settings.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<AuthResult> updateAdminAccount({
    required String currentPassword,
    required String name,
    required String phone,
    required String email,
    String? password,
  }) async {
    final data = await _send(
      'PUT',
      'api/admin/account',
      body: {
        'currentPassword': currentPassword,
        'name': name.trim(),
        'phone': phone.trim(),
        'email': email.trim(),
        if ((password ?? '').isNotEmpty) 'password': password,
      },
    );
    return AuthResult.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<List<SupportConversation>> supportConversations() async {
    final data = await _send('GET', 'api/support/conversations');
    return _parseList(data, SupportConversation.fromJson);
  }

  Future<SupportConversation> createSupportConversation() async {
    final data = await _send('POST', 'api/support/conversations');
    return SupportConversation.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<SupportConversationDetail> supportConversation(int id) async {
    final data = await _send('GET', 'api/support/conversations/$id');
    return SupportConversationDetail.fromJson(
      (data as Map).cast<String, dynamic>(),
    );
  }

  Future<SupportChatResult> sendSupportMessage(
    int conversationId,
    String text,
  ) async {
    final data = await _send(
      'POST',
      'api/support/conversations/$conversationId/messages',
      body: {'text': text},
    );
    return SupportChatResult.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<SupportTicket> createSupportTicket({
    required int conversationId,
    required String contactName,
    required String phone,
    required String email,
    required String preferredContact,
    required String category,
    required String subject,
    required String description,
  }) async {
    final data = await _send(
      'POST',
      'api/support/conversations/$conversationId/tickets',
      body: {
        'contactName': contactName,
        'phone': phone,
        'email': email.trim().isEmpty ? null : email.trim(),
        'preferredContact': preferredContact,
        'category': category,
        'subject': subject,
        'description': description,
      },
    );
    return SupportTicket.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<List<SupportTicket>> supportTickets() async {
    final data = await _send('GET', 'api/support/tickets');
    return _parseList(data, SupportTicket.fromJson);
  }

  Future<SupportTicketDetail> supportTicket(int id) async {
    final data = await _send('GET', 'api/support/tickets/$id');
    return SupportTicketDetail.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<SupportMessage> sendSupportTicketMessage(
    int ticketId,
    String text,
  ) async {
    final data = await _send(
      'POST',
      'api/support/tickets/$ticketId/messages',
      body: {'text': text},
    );
    return SupportMessage.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<SupportTicket> resolveSupportTicket(int id) async {
    final data = await _send('PATCH', 'api/support/tickets/$id/resolve');
    return SupportTicket.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<List<SupportTicket>> adminSupportTickets() async {
    final data = await _send('GET', 'api/admin/support/tickets');
    return _parseList(data, SupportTicket.fromJson);
  }

  Future<SupportTicketDetail> adminSupportTicket(int id) async {
    final data = await _send('GET', 'api/admin/support/tickets/$id');
    return SupportTicketDetail.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<SupportTicket> updateAdminSupportTicket(
    int id, {
    String? status,
    int? assignedAdminId,
    bool clearAssignedAdmin = false,
    String? priority,
  }) async {
    final data = await _send(
      'PATCH',
      'api/admin/support/tickets/$id',
      body: {
        'status': ?status,
        'assignedAdminId': ?assignedAdminId,
        'clearAssignedAdmin': clearAssignedAdmin,
        'priority': ?priority,
      },
    );
    return SupportTicket.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<SupportMessage> adminSupportReply(
    int ticketId,
    String text, {
    bool internalNote = false,
  }) async {
    final data = await _send(
      'POST',
      'api/admin/support/tickets/$ticketId/messages',
      body: {'text': text, 'internalNote': internalNote},
    );
    return SupportMessage.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<List<SupportKnowledgeEntry>> supportKnowledge() async {
    final data = await _send('GET', 'api/admin/support/knowledge');
    return _parseList(data, SupportKnowledgeEntry.fromJson);
  }

  Future<SupportKnowledgeEntry> saveSupportKnowledge({
    int? id,
    required String title,
    required String category,
    required String content,
    required String keywords,
    required bool active,
  }) async {
    final data = await _send(
      id == null ? 'POST' : 'PUT',
      id == null
          ? 'api/admin/support/knowledge'
          : 'api/admin/support/knowledge/$id',
      body: {
        'title': title,
        'category': category,
        'content': content,
        'keywords': keywords,
        'active': active,
      },
    );
    return SupportKnowledgeEntry.fromJson(
      (data as Map).cast<String, dynamic>(),
    );
  }

  Future<void> deleteSupportKnowledge(int id) async {
    await _send('DELETE', 'api/admin/support/knowledge/$id');
  }

  Future<void> registerPushToken(String token) async {
    await _send(
      'POST',
      'api/push/register',
      body: {
        'token': token,
        'platform': defaultTargetPlatform == TargetPlatform.iOS
            ? 'ios'
            : 'android',
      },
    );
  }
}
