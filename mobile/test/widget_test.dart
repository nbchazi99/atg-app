import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:atg_mobile/format.dart';
import 'package:atg_mobile/models.dart';
import 'package:atg_mobile/session.dart';
import 'package:atg_mobile/theme.dart';

void main() {
  test('dark theme keeps primary text high contrast against its surface', () {
    final theme = atgTheme(darkMode: true);
    final backgroundLuminance = theme.scaffoldBackgroundColor
        .computeLuminance();
    final textLuminance = theme.colorScheme.onSurface.computeLuminance();
    final contrastRatio = (textLuminance + 0.05) / (backgroundLuminance + 0.05);

    expect(theme.brightness, Brightness.dark);
    expect(contrastRatio, greaterThan(7));
    atgTheme(darkMode: false);
  });

  test('dark theme keeps text legible on colored controls', () {
    final scheme = atgTheme(darkMode: true).colorScheme;
    final foregrounds = [
      (scheme.primary, scheme.onPrimary),
      (scheme.secondary, scheme.onSecondary),
      (scheme.tertiary, scheme.onTertiary),
      (scheme.error, scheme.onError),
    ];

    for (final (background, foreground) in foregrounds) {
      final lighter = background.computeLuminance();
      final darker = foreground.computeLuminance();
      final contrastRatio = (lighter + 0.05) / (darker + 0.05);

      expect(contrastRatio, greaterThan(4.5));
    }
    atgTheme(darkMode: false);
  });

  test('dark mode preference persists independently of sign-out', () async {
    SharedPreferences.setMockInitialValues({});
    final session = await Session.load();

    expect(session.isDarkMode, isFalse);
    await session.saveDarkMode(true);
    await session.logout();

    expect(session.isDarkMode, isTrue);
  });

  test('settings keep EcoCash as the only supported payment method', () {
    final settings = Settings.fromJson(const {});

    expect(settings.depositMethods, ['EcoCash']);
    expect(settings.withdrawalMethods, ['EcoCash']);
  });

  test('settings default a missing withdrawal fee to ten percent', () {
    final settings = Settings.fromJson(const {});

    expect(settings.withdrawFeePct, 10);
  });

  test('settings ignore legacy payment methods from the server', () {
    final settings = Settings.fromJson({
      'depositMethods': ['Cash', 'EcoCash'],
      'withdrawalMethods': ['SmileCash'],
    });

    expect(settings.depositMethods, ['EcoCash']);
    expect(settings.withdrawalMethods, ['EcoCash']);
  });

  test('money groups thousands and keeps two decimals', () {
    expect(money(25), r'$25.00');
    expect(money(1234.5), r'$1,234.50');
    expect(money(0), r'$0.00');
  });

  test('price keeps more decimals for small and mid prices', () {
    expect(price(1.08432), '1.08432');
    expect(price(238.4), '238.400');
    expect(price(61234.5), '61,234.50');
  });

  test('pct always carries a sign', () {
    expect(pct(0.42), '+0.42%');
    expect(pct(-1.5), '-1.50%');
  });

  test('timeAgo treats timezone-less server timestamps as UTC', () {
    final now = DateTime.utc(2026, 11, 5, 19, 0, 45);

    expect(timeAgo('2026-11-05T19:00:00', now: now), 'just now');
    expect(timeAgo('2026-11-05T21:00:00+02:00', now: now), 'just now');
    expect(timeAgo('2026-11-05T18:58:00Z', now: now), '2m ago');
  });

  test('timeAgo compares server timestamps with an offset local clock', () {
    final localNow = DateTime.parse('2026-11-05T21:00:45+02:00');

    expect(timeAgo('2026-11-05T19:00:00', now: localNow), 'just now');
    expect(timeAgo('2026-11-05T18:00:00', now: localNow), '1h ago');
  });

  test('initials uses the first and last name', () {
    expect(initials('Blessed Moyo'), 'BM');
    expect(initials('Blessed'), 'B');
    expect(initials(''), 'A');
  });

  test('membership labels match the API values', () {
    expect(membershipLabel('DERIV_VIP'), 'Deriv VIP');
    expect(membershipLabel('WELTRADE_VIP'), 'Weltrade VIP');
    expect(membershipLabel('NONE'), 'No membership');
  });

  test('greeting follows the local hour', () {
    DateTime at(int hour, [int minute = 0]) =>
        DateTime(2026, 11, 2, hour, minute);

    expect(greeting(at(4, 59)), 'Still up');
    expect(greeting(at(5)), 'Good morning');
    expect(greeting(at(11, 59)), 'Good morning');
    expect(greeting(at(12)), 'Good afternoon');
    expect(greeting(at(16, 59)), 'Good afternoon');
    expect(greeting(at(17)), 'Good evening');
    expect(greeting(at(21, 59)), 'Good evening');
    expect(greeting(at(22)), 'Still up');
    expect(greeting(at(0, 30)), 'Still up');
  });

  test('day label spells out the weekday and date', () {
    expect(dayLabel(DateTime(2026, 11, 1)), 'SUNDAY · 1 NOVEMBER');
    expect(dayLabel(DateTime(2026, 1, 31)), 'SATURDAY · 31 JANUARY');
  });
}
