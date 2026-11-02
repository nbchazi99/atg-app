import 'dart:async';

import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'api.dart';
import 'app_state.dart' as state;
import 'push.dart';
import 'realtime.dart';
import 'screens/auth_screen.dart';
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
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.dark,
      statusBarBrightness: Brightness.light,
      systemNavigationBarColor: AtgColors.bg,
      systemNavigationBarIconBrightness: Brightness.dark,
    ),
  );
  state.session = await Session.load();
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
  bool _splash = true;
  Timer? _splashTimer;

  @override
  void initState() {
    super.initState();
    state.session.addListener(_sessionChanged);
    if (_loggedIn) {
      _refreshSession();
      PushService.start(state.session, state.api);
    }
    _splashTimer = Timer(const Duration(milliseconds: 2200), () {
      if (mounted) setState(() => _splash = false);
    });
  }

  Future<void> _refreshSession() async {
    try {
      final user = await state.api.me();
      await state.session.saveUser(user);
    } on ApiException catch (e) {
      if (e.status == 401) await state.session.logout();
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
    if (loggedIn != _loggedIn) {
      setState(() => _loggedIn = loggedIn);
      if (loggedIn) {
        PushService.start(state.session, state.api);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ATG',
      debugShowCheckedModeBanner: false,
      theme: atgTheme(),
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
