import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';
import 'package:image_picker/image_picker.dart';

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
      final streamed = await _client
          .send(request)
          .timeout(const Duration(seconds: 30));
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
      throw const ApiException('Your session ended. Sign in again.', 401);
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

  Future<AppUser> me() async {
    final data = await _send('GET', 'api/me');
    return AppUser.fromJson((data as Map).cast<String, dynamic>());
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

  Future<List<MarketPair>> markets(String category) async {
    final data = await _send(
      'GET',
      'api/markets',
      query: {'category': category},
    );
    return _parseList(data, MarketPair.fromJson);
  }

  Future<PairDetail> pair(String symbol) async {
    final data = await _send(
      'GET',
      'api/markets/${Uri.encodeComponent(symbol)}',
    );
    return PairDetail.fromJson((data as Map).cast<String, dynamic>());
  }

  Future<bool> toggleFavourite(String symbol) async {
    final data = await _send(
      'POST',
      'api/markets/${Uri.encodeComponent(symbol)}/favourite',
    );
    return data is Map && data['favourite'] == true;
  }

  Future<List<Room>> rooms() async {
    final data = await _send('GET', 'api/rooms');
    return _parseList(data, Room.fromJson);
  }

  Future<List<FeedItem>> feed(String filter) async {
    final data = await _send(
      'GET',
      'api/rooms/feed',
      query: {'filter': filter},
    );
    return _parseList(data, FeedItem.fromJson);
  }

  Future<List<RoomMessage>> messages(int roomId) async {
    final data = await _send('GET', 'api/rooms/$roomId/messages');
    return _parseList(data, RoomMessage.fromJson);
  }

  Future<RoomMessage> sendMessage(int roomId, String text) async {
    final data = await _send(
      'POST',
      'api/rooms/$roomId/messages',
      body: {'text': text},
    );
    return RoomMessage.fromJson((data as Map).cast<String, dynamic>());
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
    final uri = Uri.parse('${session.server}api/uploads');
    final request = http.MultipartRequest('POST', uri);
    final token = session.token;
    if (token != null && token.isNotEmpty) {
      request.headers['Authorization'] = 'Bearer $token';
    }

    final name = file.name.isEmpty ? 'proof.jpg' : file.name;
    final lower = name.toLowerCase();
    final mime = lower.endsWith('.png')
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
        const Duration(seconds: 60),
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
          : 'Could not upload that image (${response.statusCode})';
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
      body: {
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
      },
    );
    return Signal.fromJson((data as Map).cast<String, dynamic>());
  }

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
}
