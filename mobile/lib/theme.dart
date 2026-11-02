import 'package:flutter/cupertino.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';

abstract final class AtgColors {
  static const bg = Color(0xFFF5F4EE);
  static const surface = Color(0xFFFFFFFF);
  static const surfaceAlt = Color(0xFFFBFAF5);
  static const ink = Color(0xFF111111);
  static const inkSoft = Color(0xFF383833);
  static const muted = Color(0xFF6F6E67);
  static const gold = Color(0xFF8A6A1F);
  static const goldDark = Color(0xFF6E5418);
  static const primary = Color(0xFFF4A81D);
  static const primaryDark = Color(0xFFD98F0F);
  static const primarySoft = Color(0xFFFCEBCB);
  static const stroke = Color(0xFFE5E4DB);
  static const strokeSoft = Color(0xFFEFEEE7);
  static const greenBg = Color(0xFFDCF3E3);
  static const greenText = Color(0xFF1B7A3D);
  static const redBg = Color(0xFFFBE3E1);
  static const redText = Color(0xFFB3261E);
  static const greyChip = Color(0xFFEFEEE8);
  static const black = Color(0xFF000000);
}

abstract final class AtgText {
  static const pageTitle = TextStyle(
    fontSize: 24,
    fontWeight: FontWeight.w800,
    color: AtgColors.ink,
    height: 1.15,
  );
  static const h1 = TextStyle(
    fontSize: 17,
    fontWeight: FontWeight.w700,
    color: AtgColors.ink,
    height: 1.25,
  );
  static const h2 = TextStyle(
    fontSize: 15,
    fontWeight: FontWeight.w700,
    color: AtgColors.ink,
    height: 1.3,
  );
  static const h3 = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.w700,
    color: AtgColors.ink,
    height: 1.3,
  );
  static const body = TextStyle(
    fontSize: 14,
    color: AtgColors.inkSoft,
    height: 1.5,
  );
  static const bodySmall = TextStyle(
    fontSize: 13,
    color: AtgColors.inkSoft,
    height: 1.45,
  );
  static const small = TextStyle(
    fontSize: 12,
    color: AtgColors.muted,
    height: 1.4,
  );
  static const tiny = TextStyle(
    fontSize: 11,
    color: AtgColors.muted,
    height: 1.35,
  );

  static const label = TextStyle(
    fontFamily: 'monospace',
    fontFamilyFallback: ['Menlo', 'Courier New'],
    fontSize: 10,
    letterSpacing: 1.5,
    fontWeight: FontWeight.w700,
    color: AtgColors.gold,
  );

  static const labelMuted = TextStyle(
    fontFamily: 'monospace',
    fontFamilyFallback: ['Menlo', 'Courier New'],
    fontSize: 10,
    letterSpacing: 1.4,
    fontWeight: FontWeight.w600,
    color: AtgColors.muted,
  );
}

ThemeData atgTheme() {
  const scheme = ColorScheme(
    brightness: Brightness.light,
    primary: AtgColors.primary,
    onPrimary: AtgColors.ink,
    primaryContainer: AtgColors.primarySoft,
    onPrimaryContainer: AtgColors.goldDark,
    secondary: AtgColors.gold,
    onSecondary: Colors.white,
    secondaryContainer: AtgColors.primarySoft,
    onSecondaryContainer: AtgColors.gold,
    tertiary: AtgColors.greenText,
    onTertiary: Colors.white,
    error: AtgColors.redText,
    onError: Colors.white,
    surface: AtgColors.surface,
    onSurface: AtgColors.ink,
    onSurfaceVariant: AtgColors.muted,
    outline: AtgColors.stroke,
    outlineVariant: AtgColors.strokeSoft,
    shadow: Color(0x14000000),
    scrim: Color(0x33000000),
    inverseSurface: AtgColors.ink,
    onInverseSurface: Colors.white,
    inversePrimary: AtgColors.primaryDark,
  );

  return ThemeData(
    useMaterial3: true,
    colorScheme: scheme,
    scaffoldBackgroundColor: AtgColors.bg,
    splashFactory: InkRipple.splashFactory,
    pageTransitionsTheme: const PageTransitionsTheme(
      builders: {
        TargetPlatform.android: ZoomPageTransitionsBuilder(),
        TargetPlatform.iOS: CupertinoPageTransitionsBuilder(),
        TargetPlatform.macOS: CupertinoPageTransitionsBuilder(),
      },
    ),
    textSelectionTheme: const TextSelectionThemeData(
      cursorColor: AtgColors.gold,
      selectionColor: AtgColors.primarySoft,
      selectionHandleColor: AtgColors.primaryDark,
    ),
    snackBarTheme: SnackBarThemeData(
      backgroundColor: AtgColors.ink,
      contentTextStyle: const TextStyle(
        color: Colors.white,
        fontSize: 13,
        height: 1.4,
      ),
      behavior: SnackBarBehavior.floating,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      insetPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
    ),
    dividerTheme: const DividerThemeData(
      color: AtgColors.strokeSoft,
      thickness: 1,
      space: 1,
    ),
  );
}

/// Lets a mouse or trackpad drag scrollables on web and desktop builds, and
/// keeps the subtle scrollbar on wide screens.
class AtgScrollBehavior extends MaterialScrollBehavior {
  const AtgScrollBehavior();

  @override
  Set<PointerDeviceKind> get dragDevices => const {
    PointerDeviceKind.touch,
    PointerDeviceKind.mouse,
    PointerDeviceKind.trackpad,
    PointerDeviceKind.stylus,
  };

  @override
  Widget buildScrollbar(
    BuildContext context,
    Widget child,
    ScrollableDetails details,
  ) {
    return Scrollbar(controller: details.controller, child: child);
  }
}

BoxDecoration cardDecoration({
  Color color = AtgColors.surface,
  Color border = AtgColors.stroke,
  double radius = 18,
}) {
  return BoxDecoration(
    color: color,
    borderRadius: BorderRadius.circular(radius),
    border: Border.all(color: border),
  );
}
