String _group(String digits) {
  final buffer = StringBuffer();
  for (var i = 0; i < digits.length; i++) {
    if (i > 0 && (digits.length - i) % 3 == 0) buffer.write(',');
    buffer.write(digits[i]);
  }
  return buffer.toString();
}

String money(num value) {
  final fixed = value.toDouble().toStringAsFixed(2);
  final parts = fixed.split('.');
  return '\$${_group(parts[0])}.${parts[1]}';
}

String price(num value) {
  final v = value.toDouble();
  final decimals = v.abs() < 10 ? 5 : (v.abs() < 1000 ? 3 : 2);
  final fixed = v.toStringAsFixed(decimals);
  final parts = fixed.split('.');
  return '${_group(parts[0])}.${parts[1]}';
}

String pct(num value) {
  final v = value.toDouble();
  return '${v >= 0 ? '+' : ''}${v.toStringAsFixed(2)}%';
}

double parseAmount(String? raw, [double fallback = 0]) {
  if (raw == null) return fallback;
  final cleaned = raw.replaceAll(RegExp(r'[^0-9.]'), '');
  if (cleaned.isEmpty) return fallback;
  return double.tryParse(cleaned) ?? fallback;
}

String timeAgo(String? iso) {
  if (iso == null || iso.trim().isEmpty) return '';
  final parsed = DateTime.tryParse(iso);
  if (parsed == null) return '';
  final minutes = DateTime.now().difference(parsed.toLocal()).inMinutes;
  if (minutes < 1) return 'just now';
  if (minutes < 60) return '${minutes}m ago';
  final hours = minutes ~/ 60;
  if (hours < 24) return '${hours}h ago';
  final days = hours ~/ 24;
  if (days == 1) return '1 d ago';
  return '$days d ago';
}

String initials(String? name) {
  final trimmed = (name ?? '').trim();
  if (trimmed.isEmpty) return 'A';
  final parts = trimmed.split(RegExp(r'\s+'));
  if (parts.length == 1) return parts.first.substring(0, 1).toUpperCase();
  return (parts.first.substring(0, 1) + parts.last.substring(0, 1))
      .toUpperCase();
}

String membershipLabel(String? membership) {
  switch (membership) {
    case 'DERIV_VIP':
      return 'Deriv VIP';
    case 'WELTRADE_VIP':
      return 'Weltrade VIP';
    default:
      return 'No membership';
  }
}

String trimNumber(num value) {
  if (value == value.roundToDouble()) return value.toInt().toString();
  return value.toStringAsFixed(2);
}

String firstName(String? name) {
  final trimmed = (name ?? '').trim();
  if (trimmed.isEmpty) return 'Trader';
  return trimmed.split(RegExp(r'\s+')).first;
}

String greeting(DateTime now) {
  final hour = now.hour;
  if (hour >= 5 && hour < 12) return 'Good morning';
  if (hour >= 12 && hour < 17) return 'Good afternoon';
  if (hour >= 17 && hour < 22) return 'Good evening';
  return 'Still up';
}

String dayLabel(DateTime now) {
  const weekdays = [
    'MONDAY',
    'TUESDAY',
    'WEDNESDAY',
    'THURSDAY',
    'FRIDAY',
    'SATURDAY',
    'SUNDAY',
  ];
  const months = [
    'JANUARY',
    'FEBRUARY',
    'MARCH',
    'APRIL',
    'MAY',
    'JUNE',
    'JULY',
    'AUGUST',
    'SEPTEMBER',
    'OCTOBER',
    'NOVEMBER',
    'DECEMBER',
  ];
  return '${weekdays[now.weekday - 1]} · ${now.day} ${months[now.month - 1]}';
}

String displaySymbol(String symbol) {
  final upper = symbol.toUpperCase();
  if (upper.length == 6) {
    return '${upper.substring(0, 3)}/${upper.substring(3)}';
  }
  return upper;
}
