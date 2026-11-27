import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'format.dart';
import 'models.dart';

class Session extends ChangeNotifier {
  Session._(this._prefs);

  final SharedPreferences _prefs;

  static Future<Session> load() async {
    final prefs = await SharedPreferences.getInstance();
    return Session._(prefs);
  }

  /// The API this build talks to.
  ///
  /// Defaults to the local backend (`http://localhost:8080/`) so a dev build works
  /// against `gradle bootRun`. Override without editing code:
  ///
  ///   flutter run --dart-define=ATG_API=https://atg-api-5de3.onrender.com/
  static String get defaultServer {
    const fromEnv = String.fromEnvironment('ATG_API');
    return fromEnv.isEmpty ? 'http://localhost:8080/' : fromEnv;
  }

  String get server => defaultServer;

  String? get token => _prefs.getString('token');

  bool get isLoggedIn => token != null && token!.isNotEmpty;
  bool get isDarkMode => _prefs.getBool('darkMode') ?? false;

  Future<void> saveDarkMode(bool darkMode) async {
    final saved = await _prefs.setBool('darkMode', darkMode);
    if (!saved) {
      throw StateError('Could not save the appearance preference.');
    }
    notifyListeners();
  }

  Future<void> saveAuth(String token, AppUser user) async {
    await _prefs.setString('token', token);
    await _saveUser(user);
    notifyListeners();
  }

  Future<void> saveUser(AppUser user) async {
    await _saveUser(user);
    notifyListeners();
  }

  Future<void> _saveUser(AppUser user) async {
    await _prefs.setInt('id', user.id);
    await _prefs.setString('name', user.name);
    if (user.username == null || user.username!.isEmpty) {
      await _prefs.remove('username');
    } else {
      await _prefs.setString('username', user.username!);
    }
    if (user.country == null || user.country!.isEmpty) {
      await _prefs.remove('country');
    } else {
      await _prefs.setString('country', user.country!);
    }
    await _prefs.setString('email', user.email);
    await _prefs.setString('phone', user.phone);
    await _prefs.setString('role', user.role);
    await _prefs.setString('membership', user.membership);
    await _prefs.setInt('points', user.points);
    if (user.avatarUrl == null || user.avatarUrl!.isEmpty) {
      await _prefs.remove('avatarUrl');
    } else {
      await _prefs.setString('avatarUrl', user.avatarUrl!);
    }
  }

  Future<void> savePoints(int points) async {
    await _prefs.setInt('points', points);
    notifyListeners();
  }

  Future<void> logout() async {
    await _prefs.remove('token');
    await _prefs.remove('id');
    await _prefs.remove('name');
    await _prefs.remove('username');
    await _prefs.remove('country');
    await _prefs.remove('email');
    await _prefs.remove('phone');
    await _prefs.remove('role');
    await _prefs.remove('membership');
    await _prefs.remove('points');
    await _prefs.remove('avatarUrl');
    notifyListeners();
  }

  int get id => _prefs.getInt('id') ?? 0;

  String get name => _prefs.getString('name') ?? 'Trader';
  String get username => _prefs.getString('username') ?? '';
  String get country => _prefs.getString('country') ?? '';
  String get email => _prefs.getString('email') ?? '';
  String get phone => _prefs.getString('phone') ?? '';
  String get role => _prefs.getString('role') ?? 'USER';
  String get membership => _prefs.getString('membership') ?? 'NONE';
  int get points => _prefs.getInt('points') ?? 0;
  String? get avatarUrl {
    final value = _prefs.getString('avatarUrl');
    return value == null || value.isEmpty ? null : value;
  }

  bool get isAdmin => role == 'ADMIN';
  String get initialsText => initials(name);
  String get membershipText => membershipLabel(membership);
}
