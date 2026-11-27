import 'dart:async';

import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'api.dart';
import 'app_state.dart' as state;
import 'push.dart';
import 'realtime.dart';
import 'screens/auth_screen.dart';
import 'screens/payment_checkout_screen.dart';
import 'screens/shell.dart';
import 'screens/splash_screen.dart';
import 'session.dart';
import 'theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try {
    await Firebase.initializeApp();
  } catch (error) {
    debugPrint('ATG firebase init failed: $error');
  }
  state.session = await Session.load();
  _setSystemUiOverlayStyle(state.session.isDarkMode);
  state.api = Api(state.session);
  state.realtime = Realtime(
    server: () => state.session.server,
    token: () => state.session.token ?? '',
  )..start();
  state.session.addListener(state.realtime.sync);
  runApp(const AtgApp());
}

class AtgApp extends StatefulWidget {
  const AtgApp({super.key});

  @override
  State<AtgApp> createState() => _AtgAppState();
}

class _AtgAppState extends State<AtgApp> {
  late bool _loggedIn = state.session.isLoggedIn;
  late bool _darkMode = state.session.isDarkMode;
  late String _profileSnapshot = _currentProfileSnapshot;
  bool _splash = true;
  Timer? _splashTimer;

  /// Set when Dischub redirects the browser back to the app with
  /// `?order=PMT…`; opened once the splash finishes.
  String? _pendingPaymentOrder = Uri.base.queryParameters['order'];

  @override
  void initState() {
    super.initState();
    state.session.addListener(_sessionChanged);
    if (_loggedIn) {
      _refreshSession();
      PushService.start(state.session, state.api);
    }
    _splashTimer = Timer(const Duration(milliseconds: 2200), () {
      if (!mounted) return;
      setState(() => _splash = false);
      _openPendingPayment();
    });
  }

  void _openPendingPayment() {
    final orderId = _pendingPaymentOrder;
    if (orderId == null || orderId.isEmpty) return;
    _pendingPaymentOrder = null;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) => PaymentResultScreen(orderId: orderId),
        ),
      );
    });
  }

  Future<void> _refreshSession() async {
    try {
      final user = await state.api.me();
      await state.session.saveUser(user);
    } on ApiException catch (e) {
      if (e.status == 401 || e.status == 403) {
        await state.session.logout();
      }
    }
  }

  @override
  void dispose() {
    _splashTimer?.cancel();
    state.session.removeListener(_sessionChanged);
    super.dispose();
  }

  void _sessionChanged() {
    final loggedIn = state.session.isLoggedIn;
    final darkMode = state.session.isDarkMode;
    final profileSnapshot = _currentProfileSnapshot;
    if (loggedIn != _loggedIn ||
        darkMode != _darkMode ||
        profileSnapshot != _profileSnapshot) {
      setState(() {
        _loggedIn = loggedIn;
        _darkMode = darkMode;
        _profileSnapshot = profileSnapshot;
      });
      _setSystemUiOverlayStyle(darkMode);
      if (loggedIn) {
        PushService.start(state.session, state.api);
      }
    }
  }

  String get _currentProfileSnapshot => [
    state.session.id,
    state.session.name,
    state.session.username,
    state.session.country,
    state.session.email,
    state.session.phone,
    state.session.avatarUrl ?? '',
    state.session.role,
    state.session.membership,
    state.session.points,
  ].join('\u0000');

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ATG',
      debugShowCheckedModeBanner: false,
      theme: atgTheme(darkMode: _darkMode),
      scrollBehavior: const AtgScrollBehavior(),
      home: AnimatedSwitcher(
        duration: const Duration(milliseconds: 420),
        switchInCurve: Curves.easeOut,
        switchOutCurve: Curves.easeIn,
        child: _splash
            ? const SplashScreen(key: ValueKey('splash'))
            : _loggedIn
            ? const AppShell(key: ValueKey('shell'))
            : const AuthScreen(key: ValueKey('auth')),
      ),
    );
  }
}

void _setSystemUiOverlayStyle(bool darkMode) {
  SystemChrome.setSystemUIOverlayStyle(
    SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: darkMode ? Brightness.light : Brightness.dark,
      statusBarBrightness: darkMode ? Brightness.dark : Brightness.light,
      systemNavigationBarColor: darkMode
          ? const Color(0xFF101216)
          : const Color(0xFFF5F4EE),
      systemNavigationBarIconBrightness: darkMode
          ? Brightness.light
          : Brightness.dark,
    ),
  );
}
