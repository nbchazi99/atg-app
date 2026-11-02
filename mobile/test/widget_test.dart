import 'package:flutter_test/flutter_test.dart';

import 'package:atg_mobile/format.dart';

void main() {
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
