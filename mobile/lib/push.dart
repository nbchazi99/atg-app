import 'dart:async';

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';

import 'api.dart';
import 'session.dart';

class PushService {
  PushService._(this._session, this._api);

  final Session _session;
  final Api _api;
  StreamSubscription<String>? _tokenRefreshSub;
  StreamSubscription<RemoteMessage>? _messageSub;

  static PushService? _instance;

  static Future<PushService> start(Session session, Api api) async {
    _instance ??= PushService._(session, api);
    await _instance!._start();
    return _instance!;
  }

  Future<void> _start() async {
    final messaging = FirebaseMessaging.instance;
    await _requestPermission(messaging);

    await _messageSub?.cancel();
    _messageSub = FirebaseMessaging.onMessage.listen((message) {
      debugPrint(
        'ATG push received: ${message.notification?.title ?? '(data)'}',
      );
    });

    await _tokenRefreshSub?.cancel();
    _tokenRefreshSub = messaging.onTokenRefresh.listen(_register);

    try {
      final token = await messaging.getToken();
      if (token != null && token.isNotEmpty) {
        await _register(token);
      }
    } catch (error) {
      debugPrint('ATG push token lookup failed: $error');
    }
  }

  Future<void> _requestPermission(FirebaseMessaging messaging) async {
    try {
      final settings = await messaging.requestPermission(
        alert: true,
        badge: true,
        sound: true,
      );
      debugPrint('ATG push permission: ${settings.authorizationStatus}');
    } catch (error) {
      debugPrint('ATG push permission request failed: $error');
    }
    try {
      await messaging.setForegroundNotificationPresentationOptions(
        alert: true,
        badge: true,
        sound: true,
      );
    } catch (error) {
      debugPrint('ATG push foreground options failed: $error');
    }
  }

  Future<void> _register(String token) async {
    debugPrint('ATG FCM token: $token');
    if (!_session.isLoggedIn) return;
    try {
      await _api.registerPushToken(token);
      debugPrint('ATG push token registered with server');
    } catch (error) {
      debugPrint('ATG push token registration failed: $error');
    }
  }
}
