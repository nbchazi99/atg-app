import 'package:flutter/material.dart';

/// Width breakpoints shared by the whole app.
abstract final class AtgBreakpoints {
  static const phone = 600.0;
  static const tablet = 900.0;
  static const desktop = 1280.0;
}

class AtgLayout {
  const AtgLayout._(this.width);

  factory AtgLayout.of(BuildContext context) =>
      AtgLayout._(MediaQuery.sizeOf(context).width);

  final double width;

  bool get isPhone => width < AtgBreakpoints.phone;
  bool get isTablet =>
      width >= AtgBreakpoints.phone && width < AtgBreakpoints.desktop;
  bool get isWide => width >= AtgBreakpoints.tablet;

  /// Horizontal padding that grows with the viewport instead of staying at 16.
  double get gutter =>
      isPhone ? 16 : (width < AtgBreakpoints.desktop ? 24 : 32);

  /// Two cards fit side by side from tablet widths up.
  int get columns => width >= AtgBreakpoints.desktop ? 3 : (isWide ? 2 : 1);
}

/// Centres page content and caps its measure so text stays readable on
/// tablets, foldables and desktop web without changing the phone layout.
class PageBody extends StatelessWidget {
  const PageBody({
    super.key,
    required this.child,
    this.maxWidth = 980,
    this.alignment = Alignment.topCenter,
  });

  final Widget child;
  final double maxWidth;
  final Alignment alignment;

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: alignment,
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: maxWidth),
        child: child,
      ),
    );
  }
}

/// Small helper for screens that put two blocks side by side on wide screens
/// and stack them on phones.
class AdaptiveSplit extends StatelessWidget {
  const AdaptiveSplit({
    super.key,
    required this.first,
    required this.second,
    this.gap = 16,
    this.breakpoint = AtgBreakpoints.tablet,
    this.firstFlex = 3,
    this.secondFlex = 2,
  });

  final Widget first;
  final Widget second;
  final double gap;
  final double breakpoint;
  final int firstFlex;
  final int secondFlex;

  @override
  Widget build(BuildContext context) {
    if (MediaQuery.sizeOf(context).width < breakpoint) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          first,
          SizedBox(height: gap),
          second,
        ],
      );
    }
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(flex: firstFlex, child: first),
        SizedBox(width: gap),
        Expanded(flex: secondFlex, child: second),
      ],
    );
  }
}
