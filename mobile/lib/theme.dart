import 'package:flutter/cupertino.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';

abstract final class AtgColors {
  static bool isDark = false;

  static Color get bg =>
      isDark ? const Color(0xFF101216) : const Color(0xFFF5F4EE);
  static Color get surface =>
      isDark ? const Color(0xFF191C22) : const Color(0xFFFFFFFF);
  static Color get surfaceAlt =>
      isDark ? const Color(0xFF22262D) : const Color(0xFFFBFAF5);
  static Color get ink =>
      isDark ? const Color(0xFFF5F6F8) : const Color(0xFF111111);
  static Color get inkSoft =>
      isDark ? const Color(0xFFD6D9DF) : const Color(0xFF383833);
  static Color get muted =>
      isDark ? const Color(0xFFB0B5BF) : const Color(0xFF6F6E67);
  static const primaryForeground = Color(0xFF111111);
  static Color get gold =>
      isDark ? const Color(0xFFFFD166) : const Color(0xFF8A6A1F);
  static Color get goldDark =>
      isDark ? const Color(0xFFFFD166) : const Color(0xFF6E5418);
  static const primary = Color(0xFFF4A81D);
  static const primaryDark = Color(0xFFD98F0F);
  static Color get primarySoft =>
      isDark ? const Color(0xFF3B2E18) : const Color(0xFFFCEBCB);
  static Color get stroke =>
      isDark ? const Color(0xFF3B4049) : const Color(0xFFE5E4DB);
  static Color get strokeSoft =>
      isDark ? const Color(0xFF2C3038) : const Color(0xFFEFEEE7);
  static Color get greenBg =>
      isDark ? const Color(0xFF183B2A) : const Color(0xFFDCF3E3);
  static Color get greenText =>
      isDark ? const Color(0xFF7DE2A3) : const Color(0xFF1B7A3D);
  static Color get redBg =>
      isDark ? const Color(0xFF442422) : const Color(0xFFFBE3E1);
  static Color get redText =>
      isDark ? const Color(0xFFFF938A) : const Color(0xFFB3261E);
  static Color get greyChip =>
      isDark ? const Color(0xFF30343C) : const Color(0xFFEFEEE8);
  static const black = Color(0xFF000000);
}

abstract final class AtgText {
  static const pageTitle = TextStyle(
    fontSize: 24,
    fontWeight: FontWeight.w800,
    height: 1.15,
  );
  static const h1 = TextStyle(
    fontSize: 17,
    fontWeight: FontWeight.w700,
    height: 1.25,
  );
  static const h2 = TextStyle(
    fontSize: 15,
    fontWeight: FontWeight.w700,
    height: 1.3,
  );
  static const h3 = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.w700,
    height: 1.3,
  );
  static const body = TextStyle(fontSize: 14, height: 1.5);
  static const bodySmall = TextStyle(fontSize: 13, height: 1.45);
  static const small = TextStyle(fontSize: 12, height: 1.4);
  static const tiny = TextStyle(fontSize: 11, height: 1.35);

  static const label = TextStyle(
    fontFamily: 'monospace',
    fontFamilyFallback: ['Menlo', 'Courier New'],
    fontSize: 10,
    letterSpacing: 1.5,
    fontWeight: FontWeight.w700,
  );

  static const labelMuted = TextStyle(
    fontFamily: 'monospace',
    fontFamilyFallback: ['Menlo', 'Courier New'],
    fontSize: 10,
    letterSpacing: 1.4,
    fontWeight: FontWeight.w600,
  );
}

ThemeData atgTheme({required bool darkMode}) {
  AtgColors.isDark = darkMode;
  final scheme = ColorScheme(
    brightness: darkMode ? Brightness.dark : Brightness.light,
    primary: AtgColors.primary,
    onPrimary: AtgColors.primaryForeground,
    primaryContainer: AtgColors.primarySoft,
    onPrimaryContainer: AtgColors.goldDark,
    secondary: AtgColors.gold,
    onSecondary: AtgColors.primaryForeground,
    secondaryContainer: AtgColors.primarySoft,
    onSecondaryContainer: AtgColors.gold,
    tertiary: AtgColors.greenText,
    tertiaryContainer: AtgColors.greenBg,
    onTertiaryContainer: AtgColors.greenText,
    onTertiary: darkMode ? AtgColors.primaryForeground : Colors.white,
    error: AtgColors.redText,
    errorContainer: AtgColors.redBg,
    onErrorContainer: AtgColors.redText,
    onError: darkMode ? AtgColors.primaryForeground : Colors.white,
    surface: AtgColors.surface,
    onSurface: AtgColors.ink,
    onSurfaceVariant: AtgColors.muted,
    outline: AtgColors.stroke,
    outlineVariant: AtgColors.strokeSoft,
    shadow: darkMode ? const Color(0x66000000) : const Color(0x14000000),
    scrim: Color(0x33000000),
    inverseSurface: darkMode ? const Color(0xFFE6E1E5) : AtgColors.ink,
    onInverseSurface: darkMode ? AtgColors.primaryForeground : Colors.white,
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
    textSelectionTheme: TextSelectionThemeData(
      cursorColor: AtgColors.gold,
      selectionColor: AtgColors.primarySoft,
      selectionHandleColor: AtgColors.primaryDark,
    ),
    snackBarTheme: SnackBarThemeData(
      backgroundColor: darkMode ? AtgColors.surfaceAlt : AtgColors.ink,
      contentTextStyle: TextStyle(
        color: darkMode ? AtgColors.ink : Colors.white,
        fontSize: 13,
        height: 1.4,
      ),
      behavior: SnackBarBehavior.floating,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      insetPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
    ),
    dividerTheme: DividerThemeData(
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
  Color? color,
  Color? border,
  double radius = 18,
}) {
  return BoxDecoration(
    color: color ?? AtgColors.surface,
    borderRadius: BorderRadius.circular(radius),
    border: Border.all(color: border ?? AtgColors.stroke),
  );
}
