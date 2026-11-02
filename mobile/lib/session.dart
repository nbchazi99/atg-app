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

  static String get defaultServer {
    return 'https://atg-api-5de3.onrender.com/';
  }

  String get server => _prefs.getString('server') ?? defaultServer;

  Future<void> setServer(String value) async {
    var v = value.trim();
    if (v.isEmpty) return;
    if (!v.startsWith('http')) v = 'http://$v';
    if (!v.endsWith('/')) v = '$v/';
    await _prefs.setString('server', v);
    notifyListeners();
  }

  String? get token => _prefs.getString('token');

  bool get isLoggedIn => token != null && token!.isNotEmpty;

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
    await _prefs.setString('email', user.email);
    await _prefs.setString('phone', user.phone);
    await _prefs.setString('role', user.role);
    await _prefs.setString('membership', user.membership);
    await _prefs.setInt('points', user.points);
  }

  Future<void> savePoints(int points) async {
    await _prefs.setInt('points', points);
    notifyListeners();
  }

  Future<void> logout() async {
    await _prefs.remove('token');
    await _prefs.remove('id');
    await _prefs.remove('name');
    await _prefs.remove('email');
    await _prefs.remove('phone');
    await _prefs.remove('role');
    await _prefs.remove('membership');
    await _prefs.remove('points');
    notifyListeners();
  }

  int get id => _prefs.getInt('id') ?? 0;

  String get name => _prefs.getString('name') ?? 'Trader';
  String get email => _prefs.getString('email') ?? '';
  String get phone => _prefs.getString('phone') ?? '';
  String get role => _prefs.getString('role') ?? 'USER';
  String get membership => _prefs.getString('membership') ?? 'NONE';
  int get points => _prefs.getInt('points') ?? 0;
  bool get isAdmin => role == 'ADMIN';
  String get initialsText => initials(name);
  String get membershipText => membershipLabel(membership);
}
