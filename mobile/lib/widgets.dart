import 'package:flutter/material.dart';

import 'app_state.dart';
import 'format.dart';
import 'models.dart';
import 'theme.dart';

/// Connection state of the live updates channel. Green means the app is
/// listening on the shared database and will refresh itself as things change.
class LiveDot extends StatelessWidget {
  const LiveDot({super.key, this.showLabel = false});

  final bool showLabel;

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<bool>(
      valueListenable: realtime.connected,
      builder: (context, connected, _) {
        return Tooltip(
          message: connected ? 'Live updates on' : 'Live updates reconnecting',
          child: InkWell(
            borderRadius: BorderRadius.circular(20),
            onTap: () => showToast(
              context,
              connected
                  ? 'Live updates are on. New messages, approvals and calls appear without refreshing.'
                  : 'Live updates are reconnecting. Pull down to refresh in the meantime.',
            ),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 6),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  AnimatedContainer(
                    duration: const Duration(milliseconds: 300),
                    width: 8,
                    height: 8,
                    decoration: BoxDecoration(
                      color: connected ? AtgColors.greenText : AtgColors.muted,
                      shape: BoxShape.circle,
                      boxShadow: connected
                          ? [
                              BoxShadow(
                                color: AtgColors.greenText.withValues(
                                  alpha: 0.35,
                                ),
                                blurRadius: 6,
                                spreadRadius: 1,
                              ),
                            ]
                          : null,
                    ),
                  ),
                  if (showLabel) ...[
                    const SizedBox(width: 5),
                    Text(
                      connected ? 'LIVE' : 'SYNC',
                      style: AtgText.labelMuted.copyWith(
                        color: connected
                            ? AtgColors.greenText
                            : AtgColors.muted,
                        fontSize: 9,
                        letterSpacing: 1.1,
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

class AtgLogo extends StatelessWidget {
  const AtgLogo({super.key, this.size = 34});

  final double size;

  @override
  Widget build(BuildContext context) {
    return Image.asset(
      'assets/images/atg_mark.png',
      width: size,
      height: size,
      fit: BoxFit.contain,
      filterQuality: FilterQuality.high,
      semanticLabel: 'ATG',
    );
  }
}

class AvatarCircle extends StatelessWidget {
  const AvatarCircle({
    super.key,
    required this.text,
    this.size = 34,
    this.onTap,
  });

  final String text;
  final double size;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Container(
        width: size,
        height: size,
        alignment: Alignment.center,
        decoration: const BoxDecoration(
          color: AtgColors.primarySoft,
          shape: BoxShape.circle,
        ),
        child: Text(
          text,
          style: TextStyle(
            fontSize: size * 0.34,
            fontWeight: FontWeight.w800,
            color: AtgColors.ink,
          ),
        ),
      ),
    );
  }
}

class PointsPill extends StatelessWidget {
  const PointsPill({super.key, required this.points});

  final int points;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: AtgColors.primarySoft,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.star_rounded, size: 13, color: AtgColors.gold),
          const SizedBox(width: 4),
          Text(
            '$points',
            style: const TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w800,
              color: AtgColors.gold,
            ),
          ),
        ],
      ),
    );
  }
}

class _RoundIconButton extends StatelessWidget {
  const _RoundIconButton({
    required this.icon,
    required this.onTap,
    this.tooltip,
  });

  final IconData icon;
  final VoidCallback? onTap;
  final String? tooltip;

  @override
  Widget build(BuildContext context) {
    final button = InkWell(
      onTap: onTap ?? () {},
      borderRadius: BorderRadius.circular(24),
      child: SizedBox(
        width: 38,
        height: 38,
        child: Icon(icon, size: 21, color: AtgColors.inkSoft),
      ),
    );
    return tooltip == null ? button : Tooltip(message: tooltip!, child: button);
  }
}

class BrandHeader extends StatelessWidget {
  const BrandHeader({super.key, this.points, this.onBell, this.onAvatar});

  final int? points;
  final VoidCallback? onBell;
  final VoidCallback? onAvatar;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 10, 6),
      child: Row(
        children: [
          const AtgLogo(size: 32),
          const SizedBox(width: 9),
          const Expanded(
            child: Text(
              'ACCURATE TRADING GROUP',
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontFamily: 'monospace',
                fontFamilyFallback: ['Menlo', 'Courier New'],
                fontSize: 11,
                letterSpacing: 0.8,
                fontWeight: FontWeight.w700,
                color: AtgColors.ink,
              ),
            ),
          ),
          if (points != null) ...[
            const SizedBox(width: 6),
            PointsPill(points: points!),
          ],
          LiveDot(showLabel: MediaQuery.sizeOf(context).width >= 420),
          _RoundIconButton(
            icon: Icons.notifications_none_rounded,
            onTap: onBell,
            tooltip: 'Notifications',
          ),
          const SizedBox(width: 2),
          AvatarCircle(text: session.initialsText, onTap: onAvatar),
        ],
      ),
    );
  }
}

class TabHeader extends StatelessWidget {
  const TabHeader({
    super.key,
    required this.title,
    this.subtitle,
    this.onBell,
    this.onAvatar,
    this.trailing,
  });

  final String title;
  final String? subtitle;
  final VoidCallback? onBell;
  final VoidCallback? onAvatar;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 10, 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: AtgText.pageTitle),
                if (subtitle != null && subtitle!.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: Text(subtitle!, style: AtgText.small),
                  ),
              ],
            ),
          ),
          ?trailing,
          LiveDot(showLabel: MediaQuery.sizeOf(context).width >= 480),
          _RoundIconButton(
            icon: Icons.notifications_none_rounded,
            onTap: onBell,
            tooltip: 'Notifications',
          ),
          const SizedBox(width: 2),
          AvatarCircle(text: session.initialsText, onTap: onAvatar),
        ],
      ),
    );
  }
}

class DetailHeader extends StatelessWidget {
  const DetailHeader({
    super.key,
    required this.title,
    this.onBack,
    this.trailing,
  });

  final String title;
  final VoidCallback? onBack;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(8, 6, 12, 6),
      child: Row(
        children: [
          _RoundIconButton(
            icon: Icons.arrow_back_ios_new_rounded,
            onTap: onBack ?? () => Navigator.of(context).maybePop(),
            tooltip: 'Back',
          ),
          const SizedBox(width: 4),
          Expanded(
            child: Text(
              title,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: AtgText.h1,
            ),
          ),
          ?trailing,
        ],
      ),
    );
  }
}

class SurfaceCard extends StatelessWidget {
  const SurfaceCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(16),
    this.margin,
    this.onTap,
    this.borderColor = AtgColors.stroke,
    this.color = AtgColors.surface,
    this.radius = 18,
  });

  final Widget child;
  final EdgeInsetsGeometry padding;
  final EdgeInsetsGeometry? margin;
  final VoidCallback? onTap;
  final Color borderColor;
  final Color color;
  final double radius;

  @override
  Widget build(BuildContext context) {
    final content = Container(
      width: double.infinity,
      margin: margin,
      padding: padding,
      decoration: cardDecoration(
        color: color,
        border: borderColor,
        radius: radius,
      ),
      child: child,
    );
    if (onTap == null) return content;
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(radius),
      child: content,
    );
  }
}

class SectionLabel extends StatelessWidget {
  const SectionLabel(this.text, {super.key, this.color = AtgColors.gold});

  final String text;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Text(text, style: AtgText.label.copyWith(color: color));
  }
}

class AtgPill extends StatelessWidget {
  const AtgPill({
    super.key,
    required this.text,
    this.background = AtgColors.greyChip,
    this.foreground = AtgColors.muted,
    this.leadingDot = false,
    this.fontSize = 10,
    this.icon,
  });

  final String text;
  final Color background;
  final Color foreground;
  final bool leadingDot;
  final double fontSize;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(7),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (leadingDot) ...[
            Container(
              width: 5,
              height: 5,
              decoration: BoxDecoration(
                color: foreground,
                shape: BoxShape.circle,
              ),
            ),
            const SizedBox(width: 4),
          ],
          if (icon != null) ...[
            Icon(icon, size: fontSize + 2, color: foreground),
            const SizedBox(width: 3),
          ],
          Text(
            text,
            style: TextStyle(
              fontSize: fontSize,
              fontWeight: FontWeight.w800,
              color: foreground,
              letterSpacing: 0.2,
            ),
          ),
        ],
      ),
    );
  }
}

class SidePill extends StatelessWidget {
  const SidePill(this.side, {super.key, this.fontSize = 10});

  final String side;
  final double fontSize;

  @override
  Widget build(BuildContext context) {
    final buy = side == 'BUY';
    return AtgPill(
      text: side,
      background: buy ? AtgColors.greenBg : AtgColors.redBg,
      foreground: buy ? AtgColors.greenText : AtgColors.redText,
      fontSize: fontSize,
    );
  }
}

class LivePill extends StatelessWidget {
  const LivePill({super.key});

  @override
  Widget build(BuildContext context) {
    return const AtgPill(
      text: 'LIVE',
      background: AtgColors.greenBg,
      foreground: AtgColors.greenText,
      leadingDot: true,
    );
  }
}

({Color bg, Color fg}) statusPalette(String? status) {
  switch ((status ?? '').toUpperCase()) {
    case 'APPROVED':
    case 'COMPLETED':
    case 'LIVE':
    case 'ACTIVE':
      return (bg: AtgColors.greenBg, fg: AtgColors.greenText);
    case 'REJECTED':
    case 'SELL':
      return (bg: AtgColors.redBg, fg: AtgColors.redText);
    case 'PENDING':
    case 'BUY':
    case 'OPEN':
      return (bg: AtgColors.primarySoft, fg: AtgColors.gold);
    default:
      return (bg: AtgColors.greyChip, fg: AtgColors.muted);
  }
}

class StatusPill extends StatelessWidget {
  const StatusPill(this.status, {super.key, this.fontSize = 9.5});

  final String status;
  final double fontSize;

  @override
  Widget build(BuildContext context) {
    final palette = statusPalette(status);
    return AtgPill(
      text: status,
      background: palette.bg,
      foreground: palette.fg,
      fontSize: fontSize,
    );
  }
}

class AtgChip extends StatelessWidget {
  const AtgChip({
    super.key,
    required this.label,
    required this.selected,
    required this.onTap,
    this.height = 40,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;
  final double height;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 140),
        height: height,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: selected ? AtgColors.primarySoft : AtgColors.surface,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: selected ? AtgColors.gold : AtgColors.stroke,
            width: selected ? 1.2 : 1,
          ),
        ),
        child: Text(
          label,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: TextStyle(
            fontSize: 12.5,
            fontWeight: FontWeight.w700,
            color: selected ? AtgColors.gold : AtgColors.inkSoft,
          ),
        ),
      ),
    );
  }
}

class AtgChipRow extends StatelessWidget {
  const AtgChipRow({
    super.key,
    required this.labels,
    required this.selected,
    required this.onSelected,
  });

  final List<String> labels;
  final int selected;
  final ValueChanged<int> onSelected;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        for (var i = 0; i < labels.length; i++) ...[
          if (i > 0) const SizedBox(width: 6),
          Expanded(
            child: AtgChip(
              label: labels[i],
              selected: selected == i,
              onTap: () => onSelected(i),
            ),
          ),
        ],
      ],
    );
  }
}

class PrimaryButton extends StatelessWidget {
  const PrimaryButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.loading = false,
    this.height = 54,
    this.icon,
  });

  final String label;
  final VoidCallback? onPressed;
  final bool loading;
  final double height;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: height,
      width: double.infinity,
      child: FilledButton(
        onPressed: loading ? null : onPressed,
        style: FilledButton.styleFrom(
          backgroundColor: AtgColors.primary,
          foregroundColor: AtgColors.ink,
          disabledBackgroundColor: AtgColors.primary.withValues(alpha: 0.45),
          disabledForegroundColor: AtgColors.ink.withValues(alpha: 0.6),
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
          ),
          textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w800),
        ),
        child: loading
            ? const SizedBox(
                width: 22,
                height: 22,
                child: CircularProgressIndicator(
                  strokeWidth: 2.2,
                  color: AtgColors.ink,
                ),
              )
            : Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  if (icon != null) ...[
                    Icon(icon, size: 19),
                    const SizedBox(width: 8),
                  ],
                  Flexible(
                    child: Text(
                      label,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
      ),
    );
  }
}

class OutlineButton extends StatelessWidget {
  const OutlineButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.height = 50,
    this.icon,
    this.foreground = AtgColors.ink,
  });

  final String label;
  final VoidCallback? onPressed;
  final double height;
  final IconData? icon;
  final Color foreground;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: height,
      width: double.infinity,
      child: OutlinedButton(
        onPressed: onPressed,
        style: OutlinedButton.styleFrom(
          foregroundColor: foreground,
          backgroundColor: AtgColors.surface,
          side: const BorderSide(color: AtgColors.stroke),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
          ),
          textStyle: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            if (icon != null) ...[
              Icon(icon, size: 18),
              const SizedBox(width: 8),
            ],
            Flexible(
              child: Text(label, maxLines: 1, overflow: TextOverflow.ellipsis),
            ),
          ],
        ),
      ),
    );
  }
}

InputDecoration atgInputDecoration({
  String? label,
  String? hint,
  Widget? suffixIcon,
  String? errorText,
  bool alignLabelWithHint = false,
}) {
  OutlineInputBorder border(Color color, [double width = 1]) =>
      OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: BorderSide(color: color, width: width),
      );

  return InputDecoration(
    filled: true,
    fillColor: AtgColors.surfaceAlt,
    isDense: false,
    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
    labelText: label,
    hintText: hint,
    alignLabelWithHint: alignLabelWithHint,
    labelStyle: const TextStyle(color: AtgColors.muted, fontSize: 14),
    floatingLabelStyle: const TextStyle(
      color: AtgColors.gold,
      fontSize: 13,
      fontWeight: FontWeight.w600,
    ),
    hintStyle: const TextStyle(color: AtgColors.muted, fontSize: 14),
    suffixIcon: suffixIcon,
    errorText: errorText,
    errorStyle: const TextStyle(color: AtgColors.redText, fontSize: 11.5),
    border: border(AtgColors.stroke),
    enabledBorder: border(AtgColors.stroke),
    focusedBorder: border(AtgColors.gold, 1.5),
    errorBorder: border(AtgColors.redText),
    focusedErrorBorder: border(AtgColors.redText, 1.5),
  );
}

class AtgField extends StatelessWidget {
  const AtgField({
    super.key,
    required this.controller,
    this.label,
    this.hint,
    this.errorText,
    this.obscureText = false,
    this.keyboardType,
    this.textCapitalization = TextCapitalization.none,
    this.maxLines = 1,
    this.minLines,
    this.enabled = true,
    this.suffixIcon,
    this.onChanged,
    this.onSubmitted,
    this.autofillHints,
    this.alignLabelWithHint = false,
  });

  final TextEditingController controller;
  final String? label;
  final String? hint;
  final String? errorText;
  final bool obscureText;
  final TextInputType? keyboardType;
  final TextCapitalization textCapitalization;
  final int maxLines;
  final int? minLines;
  final bool enabled;
  final Widget? suffixIcon;
  final ValueChanged<String>? onChanged;
  final ValueChanged<String>? onSubmitted;
  final Iterable<String>? autofillHints;
  final bool alignLabelWithHint;

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      obscureText: obscureText,
      keyboardType: keyboardType,
      textCapitalization: textCapitalization,
      maxLines: obscureText ? 1 : maxLines,
      minLines: minLines,
      enabled: enabled,
      onChanged: onChanged,
      onSubmitted: onSubmitted,
      autofillHints: autofillHints,
      style: const TextStyle(fontSize: 14.5, color: AtgColors.ink),
      decoration: atgInputDecoration(
        label: label,
        hint: hint,
        errorText: errorText,
        suffixIcon: suffixIcon,
        alignLabelWithHint: alignLabelWithHint,
      ),
    );
  }
}

class EmptyState extends StatelessWidget {
  const EmptyState({
    super.key,
    required this.title,
    required this.message,
    this.icon = Icons.inbox_outlined,
  });

  final String title;
  final String message;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 44),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 52,
            height: 52,
            decoration: const BoxDecoration(
              color: AtgColors.primarySoft,
              shape: BoxShape.circle,
            ),
            child: Icon(icon, color: AtgColors.gold, size: 24),
          ),
          const SizedBox(height: 14),
          Text(
            title,
            textAlign: TextAlign.center,
            style: AtgText.h1.copyWith(fontSize: 17),
          ),
          const SizedBox(height: 6),
          Text(
            message,
            textAlign: TextAlign.center,
            style: AtgText.small.copyWith(fontSize: 13),
          ),
        ],
      ),
    );
  }
}

class LoadingBlock extends StatelessWidget {
  const LoadingBlock({super.key, this.padding = 40});

  final double padding;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.all(padding),
      child: const Center(
        child: SizedBox(
          width: 24,
          height: 24,
          child: CircularProgressIndicator(
            strokeWidth: 2.4,
            color: AtgColors.gold,
          ),
        ),
      ),
    );
  }
}

class SignalCard extends StatelessWidget {
  const SignalCard({super.key, required this.signal, this.onTap, this.margin});

  final Signal signal;
  final VoidCallback? onTap;
  final EdgeInsetsGeometry? margin;

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      margin: margin ?? const EdgeInsets.only(bottom: 12),
      onTap: onTap,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  signal.title,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: AtgText.h1,
                ),
              ),
              const SizedBox(width: 8),
              SidePill(signal.side),
              if (signal.isLive) ...[
                const SizedBox(width: 5),
                const LivePill(),
              ],
            ],
          ),
          const SizedBox(height: 5),
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(child: Text(signal.subtitle, style: AtgText.small)),
              if (signal.roomLabel.isNotEmpty) ...[
                const SizedBox(width: 8),
                AtgPill(text: signal.roomLabel.toUpperCase()),
              ],
            ],
          ),
          const SizedBox(height: 14),
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: _Level(label: 'ENTRY', value: signal.entry),
              ),
              const _LevelDivider(),
              Expanded(
                child: _Level(label: 'STOP', value: signal.stopLoss),
              ),
              const _LevelDivider(),
              Expanded(
                child: _Level(label: 'TARGETS', value: signal.targets),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(child: Text(signal.ruleName, style: AtgText.small)),
              AtgPill(
                text: '${signal.participants}',
                background: AtgColors.primarySoft,
                foreground: AtgColors.gold,
                icon: Icons.person_outline_rounded,
              ),
              const SizedBox(width: 8),
              Text(timeAgo(signal.createdAt), style: AtgText.small),
            ],
          ),
        ],
      ),
    );
  }
}

class _Level extends StatelessWidget {
  const _Level({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: AtgText.label.copyWith(letterSpacing: 1.2)),
        const SizedBox(height: 3),
        Text(
          value.isEmpty ? '—' : value,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          style: const TextStyle(
            fontSize: 13,
            color: AtgColors.ink,
            height: 1.3,
          ),
        ),
      ],
    );
  }
}

class _LevelDivider extends StatelessWidget {
  const _LevelDivider();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 1,
      height: 34,
      margin: const EdgeInsets.only(left: 10, right: 12),
      color: AtgColors.strokeSoft,
    );
  }
}

class TickerStrip extends StatelessWidget {
  const TickerStrip({super.key, required this.tickers, this.onTap});

  final List<MarketPair> tickers;
  final ValueChanged<MarketPair>? onTap;

  @override
  Widget build(BuildContext context) {
    if (tickers.isEmpty) return const SizedBox.shrink();
    return SizedBox(
      height: 66,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        itemCount: tickers.length,
        separatorBuilder: (_, _) => const SizedBox(width: 8),
        itemBuilder: (context, index) {
          final pair = tickers[index];
          final up = pair.changePct >= 0;
          return InkWell(
            onTap: onTap == null ? null : () => onTap!(pair),
            borderRadius: BorderRadius.circular(14),
            child: Container(
              width: 124,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
              decoration: cardDecoration(radius: 14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    pair.symbol,
                    style: const TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w800,
                      color: AtgColors.ink,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    price(pair.price),
                    style: const TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: AtgColors.inkSoft,
                    ),
                  ),
                  const SizedBox(height: 1),
                  Text(
                    pct(pair.changePct),
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w700,
                      color: up ? AtgColors.greenText : AtgColors.redText,
                    ),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}

class CourseCover extends StatelessWidget {
  const CourseCover({super.key, required this.url});

  final String url;

  @override
  Widget build(BuildContext context) {
    if (url.isEmpty) return const SizedBox.shrink();
    return ClipRRect(
      borderRadius: BorderRadius.circular(12),
      child: AspectRatio(
        aspectRatio: 16 / 9,
        child: Image.network(
          mediaUrl(url),
          fit: BoxFit.cover,
          filterQuality: FilterQuality.high,
          errorBuilder: (_, _, _) => Container(
            color: AtgColors.primarySoft,
            alignment: Alignment.center,
            child: const Icon(
              Icons.school_outlined,
              size: 26,
              color: AtgColors.gold,
            ),
          ),
        ),
      ),
    );
  }
}

class MarketRow extends StatelessWidget {
  const MarketRow({
    super.key,
    required this.pair,
    required this.favourite,
    required this.onTap,
    required this.onFavourite,
  });

  final MarketPair pair;
  final bool favourite;
  final VoidCallback onTap;
  final VoidCallback onFavourite;

  @override
  Widget build(BuildContext context) {
    final up = pair.changePct >= 0;
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 12),
        child: Row(
          children: [
            InkWell(
              onTap: onFavourite,
              borderRadius: BorderRadius.circular(20),
              child: SizedBox(
                width: 36,
                height: 36,
                child: Icon(
                  favourite ? Icons.star_rounded : Icons.star_border_rounded,
                  size: 21,
                  color: favourite ? AtgColors.primaryDark : AtgColors.stroke,
                ),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    pair.symbol,
                    style: const TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w800,
                      color: AtgColors.ink,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    pair.name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: AtgText.small,
                  ),
                ],
              ),
            ),
            const SizedBox(width: 10),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  price(pair.price),
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w800,
                    color: AtgColors.ink,
                  ),
                ),
                const SizedBox(height: 4),
                AtgPill(
                  text: pct(pair.changePct),
                  background: up ? AtgColors.greenBg : AtgColors.redBg,
                  foreground: up ? AtgColors.greenText : AtgColors.redText,
                  fontSize: 11,
                ),
                const SizedBox(height: 3),
                Text(timeAgo(pair.updatedAt), style: AtgText.tiny),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class TxRow extends StatelessWidget {
  const TxRow({super.key, required this.tx, this.showDivider = true});

  final WalletTx tx;
  final bool showDivider;

  @override
  Widget build(BuildContext context) {
    final withdrawal = tx.type == 'WITHDRAWAL';
    final purchase = tx.type == 'PURCHASE';
    final icon = purchase
        ? Icons.school_outlined
        : withdrawal
        ? Icons.north_east_rounded
        : Icons.south_west_rounded;
    final bg = purchase
        ? AtgColors.primarySoft
        : withdrawal
        ? AtgColors.redBg
        : AtgColors.greenBg;
    final fg = purchase
        ? AtgColors.gold
        : withdrawal
        ? AtgColors.redText
        : AtgColors.greenText;

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 11),
          child: Row(
            children: [
              Container(
                width: 38,
                height: 38,
                decoration: BoxDecoration(
                  color: bg,
                  borderRadius: BorderRadius.circular(11),
                ),
                child: Icon(icon, size: 18, color: fg),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      tx.description,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w700,
                        color: AtgColors.ink,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(timeAgo(tx.createdAt), style: AtgText.small),
                  ],
                ),
              ),
              const SizedBox(width: 10),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    money(tx.amount),
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w800,
                      color: AtgColors.ink,
                    ),
                  ),
                  const SizedBox(height: 4),
                  StatusPill(tx.status),
                ],
              ),
            ],
          ),
        ),
        if (showDivider) const Divider(height: 1),
      ],
    );
  }
}

class FeedRow extends StatelessWidget {
  const FeedRow({super.key, required this.item, required this.onTap});

  final FeedItem item;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final buy = item.isBuy;
    return SurfaceCard(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      onTap: onTap,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 38,
            height: 38,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: buy ? AtgColors.greenBg : AtgColors.redBg,
              shape: BoxShape.circle,
            ),
            child: Text(
              item.badge.isEmpty ? (buy ? 'B' : 'S') : item.badge,
              style: TextStyle(
                fontSize: 10,
                fontWeight: FontWeight.w800,
                color: buy ? AtgColors.greenText : AtgColors.redText,
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        item.title,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 14.5,
                          fontWeight: FontWeight.w700,
                          color: AtgColors.ink,
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    SidePill(item.side, fontSize: 9.5),
                  ],
                ),
                const SizedBox(height: 3),
                Text(
                  item.preview,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: AtgText.small,
                ),
                const SizedBox(height: 6),
                Row(
                  children: [
                    Text(
                      item.roomName,
                      style: AtgText.tiny.copyWith(color: AtgColors.gold),
                    ),
                    const Spacer(),
                    if (item.participants > 0) ...[
                      const Icon(
                        Icons.person_outline_rounded,
                        size: 13,
                        color: AtgColors.muted,
                      ),
                      const SizedBox(width: 3),
                      Text('${item.participants}', style: AtgText.tiny),
                      const SizedBox(width: 8),
                    ],
                    Text(timeAgo(item.createdAt), style: AtgText.tiny),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class OptionTile {
  const OptionTile({
    required this.title,
    required this.subtitle,
    required this.badge,
    this.badgeColor = AtgColors.ink,
    this.tag,
    this.logo,
  });

  final String title;
  final String subtitle;
  final String badge;
  final Color badgeColor;
  final String? tag;
  final String? logo;
}

class OptionList extends StatelessWidget {
  const OptionList({
    super.key,
    required this.options,
    required this.selected,
    required this.onSelected,
  });

  final List<OptionTile> options;
  final int selected;
  final ValueChanged<int> onSelected;

  @override
  Widget build(BuildContext context) {
    final showLogos = options.any((option) => option.logo != null);
    return Column(
      children: [
        for (var i = 0; i < options.length; i++)
          Padding(
            padding: EdgeInsets.only(bottom: i == options.length - 1 ? 0 : 10),
            child: InkWell(
              borderRadius: BorderRadius.circular(14),
              onTap: () => onSelected(i),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 140),
                padding: const EdgeInsets.all(13),
                decoration: BoxDecoration(
                  color: selected == i
                      ? AtgColors.surface
                      : AtgColors.surfaceAlt,
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(
                    color: selected == i ? AtgColors.gold : AtgColors.stroke,
                    width: selected == i ? 1.4 : 1,
                  ),
                ),
                child: Row(
                  children: [
                    if (showLogos)
                      Container(
                        width: 68,
                        height: 40,
                        alignment: Alignment.center,
                        padding: const EdgeInsets.symmetric(
                          horizontal: 5,
                          vertical: 4,
                        ),
                        decoration: BoxDecoration(
                          color: AtgColors.surface,
                          borderRadius: BorderRadius.circular(9),
                          border: Border.all(color: AtgColors.strokeSoft),
                        ),
                        child: options[i].logo == null
                            ? Text(
                                options[i].badge,
                                style: TextStyle(
                                  fontSize: 11.5,
                                  fontWeight: FontWeight.w800,
                                  color: options[i].badgeColor,
                                ),
                              )
                            : ClipRRect(
                                borderRadius: BorderRadius.circular(4),
                                child: Image.asset(
                                  options[i].logo!,
                                  fit: BoxFit.contain,
                                  filterQuality: FilterQuality.high,
                                  semanticLabel: options[i].title,
                                ),
                              ),
                      )
                    else
                      Container(
                        width: 38,
                        height: 38,
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          color: AtgColors.surface,
                          shape: BoxShape.circle,
                          border: Border.all(color: AtgColors.strokeSoft),
                        ),
                        child: Text(
                          options[i].badge,
                          style: TextStyle(
                            fontSize: 11.5,
                            fontWeight: FontWeight.w800,
                            color: options[i].badgeColor,
                          ),
                        ),
                      ),
                    const SizedBox(width: 11),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Flexible(
                                child: Text(
                                  options[i].title,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(
                                    fontSize: 14.5,
                                    fontWeight: FontWeight.w700,
                                    color: AtgColors.ink,
                                  ),
                                ),
                              ),
                              if (options[i].tag != null) ...[
                                const SizedBox(width: 6),
                                AtgPill(
                                  text: options[i].tag!,
                                  background: AtgColors.greenBg,
                                  foreground: AtgColors.greenText,
                                  fontSize: 9,
                                ),
                              ],
                            ],
                          ),
                          if (options[i].subtitle.isNotEmpty) ...[
                            const SizedBox(height: 2),
                            Text(
                              options[i].subtitle,
                              style: AtgText.small.copyWith(fontSize: 11.5),
                            ),
                          ],
                        ],
                      ),
                    ),
                    const SizedBox(width: 8),
                    Container(
                      width: 20,
                      height: 20,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        border: Border.all(
                          color: selected == i
                              ? AtgColors.gold
                              : AtgColors.stroke,
                          width: 1.5,
                        ),
                      ),
                      child: selected == i
                          ? Center(
                              child: Container(
                                width: 10,
                                height: 10,
                                decoration: const BoxDecoration(
                                  color: AtgColors.primary,
                                  shape: BoxShape.circle,
                                ),
                              ),
                            )
                          : null,
                    ),
                  ],
                ),
              ),
            ),
          ),
      ],
    );
  }
}

void showToast(BuildContext context, String message) {
  final messenger = ScaffoldMessenger.maybeOf(context);
  if (messenger == null) return;
  messenger.hideCurrentSnackBar();
  messenger.showSnackBar(SnackBar(content: Text(message)));
}

Future<void> showMessage(
  BuildContext context,
  String title,
  String message, {
  String action = 'OK',
}) {
  return showDialog<void>(
    context: context,
    builder: (dialogContext) => AlertDialog(
      backgroundColor: AtgColors.surface,
      surfaceTintColor: Colors.transparent,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      title: Text(title, style: AtgText.h1),
      content: SingleChildScrollView(child: Text(message, style: AtgText.body)),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(dialogContext).pop(),
          child: Text(
            action,
            style: const TextStyle(
              color: AtgColors.gold,
              fontWeight: FontWeight.w800,
            ),
          ),
        ),
      ],
    ),
  );
}

Future<bool> confirmAction(
  BuildContext context, {
  required String title,
  required String message,
  String confirmLabel = 'Confirm',
  String cancelLabel = 'Cancel',
  bool destructive = false,
}) async {
  final result = await showDialog<bool>(
    context: context,
    builder: (dialogContext) => AlertDialog(
      backgroundColor: AtgColors.surface,
      surfaceTintColor: Colors.transparent,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      title: Text(title, style: AtgText.h1),
      content: Text(message, style: AtgText.body),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(dialogContext).pop(false),
          child: Text(
            cancelLabel,
            style: const TextStyle(
              color: AtgColors.muted,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
        TextButton(
          onPressed: () => Navigator.of(dialogContext).pop(true),
          child: Text(
            confirmLabel,
            style: TextStyle(
              color: destructive ? AtgColors.redText : AtgColors.gold,
              fontWeight: FontWeight.w800,
            ),
          ),
        ),
      ],
    ),
  );
  return result ?? false;
}

Future<void> showServerDialog(BuildContext context) async {
  final controller = TextEditingController(text: session.server);
  final saved = await showDialog<bool>(
    context: context,
    builder: (dialogContext) => AlertDialog(
      backgroundColor: AtgColors.surface,
      surfaceTintColor: Colors.transparent,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      title: const Text('ATG server address', style: AtgText.h1),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Use 10.0.2.2 on the Android emulator, 127.0.0.1 on the iOS simulator, or your computer\'s LAN IP on a real phone.',
            style: AtgText.small,
          ),
          const SizedBox(height: 14),
          TextField(
            controller: controller,
            keyboardType: TextInputType.url,
            autocorrect: false,
            style: const TextStyle(fontSize: 14),
            decoration: atgInputDecoration(hint: 'http://192.168.1.10:8080/'),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(dialogContext).pop(false),
          child: const Text(
            'Cancel',
            style: TextStyle(
              color: AtgColors.muted,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
        TextButton(
          onPressed: () => Navigator.of(dialogContext).pop(true),
          child: const Text(
            'Save',
            style: TextStyle(
              color: AtgColors.gold,
              fontWeight: FontWeight.w800,
            ),
          ),
        ),
      ],
    ),
  );
  if (saved == true) {
    await session.setServer(controller.text);
    if (context.mounted) showToast(context, 'Server set to ${session.server}');
  }
  controller.dispose();
}

void notifyAllCaughtUp(BuildContext context) {
  showMessage(
    context,
    'Notifications',
    'You are all caught up. New calls and payment decisions appear here.',
  );
}
