import 'dart:async';
import 'dart:convert';
import 'dart:math' as math;

import 'package:flutter/foundation.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

typedef ScopeListener = void Function(String scope, Map<String, dynamic> event);

/// Live invalidation channel to the ATG API.
///
/// The server only broadcasts the *scope* that changed (a room, the signals
/// list, the caller's wallet...). Data is always re-read through the REST API,
/// so role checks and VIP room gating stay on the server.
class Realtime {
  Realtime({required this.server, required this.token});

  final String Function() server;
  final String Function() token;

  static const pingInterval = Duration(seconds: 20);
  static const _minBackoff = Duration(seconds: 1);
  static const _maxBackoff = Duration(seconds: 30);

  final ValueNotifier<bool> connected = ValueNotifier<bool>(false);

  final Map<String, Set<ScopeListener>> _listeners =
      <String, Set<ScopeListener>>{};
  final Set<VoidCallback> _onSync = <VoidCallback>{};

  WebSocketChannel? _channel;
  StreamSubscription<dynamic>? _sub;
  Timer? _ping;
  Timer? _retry;
  String? _openToken;
  int _attempt = 0;
  int _generation = 0;
  bool _running = false;
  bool _disposed = false;

  /// Subscribes to a list of scopes and re-runs [reload] when any fires.
  /// Bursts are coalesced, so a message that touches `rooms` and `room:3`
  /// only costs one request. Returns a cancel callback.
  VoidCallback bind(
    Iterable<String> scopes,
    VoidCallback reload, {
    Duration debounce = const Duration(milliseconds: 200),
  }) {
    Timer? timer;
    void fire(String _, Map<String, dynamic> _) {
      timer?.cancel();
      timer = Timer(debounce, reload);
    }

    final cancels = <VoidCallback>[
      for (final scope in scopes) listen(scope, fire),
      onSync(reload),
    ];
    return () {
      timer?.cancel();
      for (final cancel in cancels) {
        cancel();
      }
    };
  }

  VoidCallback listen(String scope, ScopeListener listener) {
    final listeners = _listeners.putIfAbsent(scope, () => <ScopeListener>{});
    listeners.add(listener);
    return () => listeners.remove(listener);
  }

  /// Runs [callback] whenever the socket (re)connects, so a screen can catch up
  /// on events it missed while offline.
  VoidCallback onSync(VoidCallback callback) {
    _onSync.add(callback);
    return () => _onSync.remove(callback);
  }

  /// Called when the session changes (sign in, sign out, server switch).
  void sync() {
    if (_disposed) return;
    final current = token().trim();
    if (current.isEmpty) {
      stop();
      return;
    }
    if (_running && current == _openToken) return;
    _teardown();
    start();
  }

  void start() {
    if (_disposed) return;
    _running = true;
    _ping ??= Timer.periodic(
      pingInterval,
      (_) => _send(const {'type': 'ping'}),
    );
    _connect();
  }

  void stop() {
    _running = false;
    _teardown();
    _ping?.cancel();
    _ping = null;
    if (connected.value) connected.value = false;
  }

  void dispose() {
    _disposed = true;
    stop();
    connected.dispose();
  }

  void _teardown() {
    _generation++;
    _retry?.cancel();
    _retry = null;
    _sub?.cancel();
    _sub = null;
    _channel?.sink.close();
    _channel = null;
    if (connected.value) connected.value = false;
  }

  Future<void> _connect() async {
    if (!_running || _disposed) return;
    final current = token().trim();
    if (current.isEmpty) return;

    final generation = ++_generation;
    _openToken = current;
    _retry?.cancel();
    _retry = null;

    try {
      final channel = WebSocketChannel.connect(_uri(current));
      _channel = channel;
      await channel.ready;
      if (generation != _generation || !_running) {
        channel.sink.close();
        return;
      }
      _attempt = 0;
      connected.value = true;
      _sub = channel.stream.listen(
        _onMessage,
        onDone: () => _onClosed(generation),
        onError: (Object _) => _onClosed(generation),
        cancelOnError: true,
      );
      for (final callback in [..._onSync]) {
        callback();
      }
    } catch (_) {
      if (generation == _generation) _scheduleRetry();
    }
  }

  void _onClosed(int generation) {
    if (generation != _generation) return;
    _channel = null;
    _sub = null;
    if (connected.value) connected.value = false;
    _scheduleRetry();
  }

  void _scheduleRetry() {
    if (!_running || _disposed) return;
    _attempt++;
    final seconds = math.min(
      _maxBackoff.inSeconds,
      _minBackoff.inSeconds * (1 << math.min(_attempt, 5)),
    );
    _retry?.cancel();
    _retry = Timer(Duration(seconds: seconds), _connect);
  }

  Uri _uri(String currentToken) {
    final base = server().trim();
    final wsBase = base.replaceFirst(RegExp('^http'), 'ws');
    final withSlash = wsBase.endsWith('/') ? wsBase : '$wsBase/';
    return Uri.parse(
      '${withSlash}ws?token=${Uri.encodeQueryComponent(currentToken)}',
    );
  }

  void _send(Map<String, Object?> message) {
    final channel = _channel;
    if (channel == null) return;
    try {
      channel.sink.add(jsonEncode(message));
    } catch (_) {
      // The stream will report the failure and trigger a reconnect.
    }
  }

  void _onMessage(dynamic raw) {
    if (raw is! String) return;
    Map<String, dynamic> message;
    try {
      final decoded = jsonDecode(raw);
      if (decoded is! Map) return;
      message = decoded.cast<String, dynamic>();
    } catch (_) {
      return;
    }
    if (message['type'] != 'event') return;
    final scope = message['scope'];
    if (scope is! String) return;
    final listeners = _listeners[scope];
    if (listeners == null || listeners.isEmpty) return;
    final event = Map<String, dynamic>.from(message)..remove('scope');
    for (final listener in [...listeners]) {
      listener(scope, event);
    }
  }
}

/// Scope names shared with the API's `RealtimeHub`.
abstract final class AtgScope {
  static const rooms = 'rooms';
  static const signals = 'signals';
  static const courses = 'courses';
  static const settings = 'settings';
  static const users = 'users';
  static const requests = 'requests';
  static const home = 'home';

  static String room(int roomId) => 'room:$roomId';
  static String wallet(int userId) => 'wallet:$userId';
  static String myHome(int userId) => 'home:$userId';
}
