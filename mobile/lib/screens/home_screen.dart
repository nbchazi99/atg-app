import 'dart:async';

import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../api.dart';
import '../app_state.dart';
import '../format.dart';
import '../models.dart';
import '../realtime.dart';
import '../responsive.dart';
import '../theme.dart';
import '../widgets.dart';
import 'academy_screen.dart';
import 'deposit_screen.dart';
import 'glossary_screen.dart';
import 'pair_detail_screen.dart';
import 'pip_calculator_screen.dart';
import 'profile_screen.dart';
import 'signal_detail_screen.dart';
import 'withdraw_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key, required this.onOpenTab});

  final ValueChanged<int> onOpenTab;

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with WidgetsBindingObserver {
  HomeData? _home;
  List<Signal> _live = const [];
  Settings? _settings;
  bool _loading = true;
  bool _failed = false;
  bool _promoDismissed = false;
  bool _inviteDismissed = false;
  String _fact = '';
  Timer? _clockTick;
  VoidCallback? _unbind;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _load();
    _scheduleClockTick();
    _unbind = realtime.bind({
      AtgScope.myHome(session.id),
      AtgScope.home,
      AtgScope.signals,
      AtgScope.settings,
      AtgScope.rooms,
    }, () => _load(silent: true));
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _clockTick?.cancel();
    _unbind?.call();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && mounted) {
      _scheduleClockTick();
      setState(() {});
    }
  }

  // Rebuild on each minute so the greeting and relative times stay honest.
  void _scheduleClockTick() {
    _clockTick?.cancel();
    final now = DateTime.now();
    final nextMinute = DateTime(
      now.year,
      now.month,
      now.day,
      now.hour,
      now.minute,
    ).add(const Duration(minutes: 1));
    _clockTick = Timer(nextMinute.difference(now), () {
      if (!mounted) return;
      setState(() {});
      _scheduleClockTick();
    });
  }

  Future<void> _load({bool silent = false}) async {
    try {
      final results = await Future.wait([
        api.home(),
        api.signals(status: 'LIVE'),
        api.settings(),
      ]);
      if (!mounted) return;
      final home = results[0] as HomeData;
      final live = results[1] as List<Signal>;
      setState(() {
        _home = home;
        _live = live.take(1).toList();
        _settings = results[2] as Settings;
        _fact = home.fact;
        _loading = false;
        _failed = false;
      });
      await session.savePoints(home.points);
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _failed = true;
      });
      if (!silent) showToast(context, e.message);
    }
  }

  void _openProfile() {
    Navigator.of(context)
        .push(MaterialPageRoute(builder: (_) => const ProfileScreen()));
  }

  void _openSignal(Signal signal) {
    Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => SignalDetailScreen(signal: signal)),
    );
  }

  void _openPair(String symbol) {
    Navigator.of(
      context,
    ).push(MaterialPageRoute(builder: (_) => PairDetailScreen(symbol: symbol)));
  }

  @override
  Widget build(BuildContext context) {
    final now = DateTime.now();
    final home = _home;
    final liveCount = home?.liveCount ?? 0;
    final name = (home != null && home.name.trim().isNotEmpty)
        ? home.name
        : session.name;
    final referralPoints = _settings?.pointsPerReferral ?? 0;
    final liveLine = home == null
        ? (_failed
              ? 'Live calls are unavailable right now.'
              : 'Checking the floor…')
        : liveCount == 0
        ? 'No calls are live right now.'
        : liveCount == 1
        ? '1 call is live.'
        : '$liveCount calls are live.';

    return SafeArea(
      bottom: false,
      child: PageBody(
        maxWidth: 820,
        child: RefreshIndicator(
          color: AtgColors.primary,
          backgroundColor: AtgColors.surface,
          onRefresh: () => _load(silent: true),
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.only(bottom: 100),
            children: [
              BrandHeader(
                points: home?.points ?? session.points,
                onBell: () => notifyAllCaughtUp(context),
                onAvatar: _openProfile,
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    SectionLabel(dayLabel(now)),
                    const SizedBox(height: 6),
                    Text(
                      '${greeting(now)}, ${firstName(name)}.',
                      style: AtgText.pageTitle,
                    ),
                    const SizedBox(height: 2),
                    Text(
                      liveLine,
                      style: const TextStyle(
                        fontSize: 19,
                        color: AtgColors.inkSoft,
                        height: 1.3,
                      ),
                    ),
                  ],
                ),
              ),
              if (!_promoDismissed &&
                  home != null &&
                  home.promotionTitle.isNotEmpty)
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
                  child: SurfaceCard(
                    borderColor: AtgColors.primary,
                    padding: const EdgeInsets.all(14),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            const Icon(
                              Icons.info_outline_rounded,
                              size: 16,
                              color: AtgColors.redText,
                            ),
                            const SizedBox(width: 6),
                            const Expanded(
                              child: Text(
                                'IMPORTANT',
                                style: TextStyle(
                                  fontFamily: 'monospace',
                                  fontFamilyFallback: ['Menlo', 'Courier New'],
                                  fontSize: 10,
                                  letterSpacing: 1.4,
                                  fontWeight: FontWeight.w800,
                                  color: AtgColors.redText,
                                ),
                              ),
                            ),
                            InkWell(
                              onTap: () =>
                                  setState(() => _promoDismissed = true),
                              borderRadius: BorderRadius.circular(16),
                              child: const Padding(
                                padding: EdgeInsets.all(4),
                                child: Icon(
                                  Icons.close_rounded,
                                  size: 16,
                                  color: AtgColors.muted,
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 2),
                        Text(home.promotionTitle, style: AtgText.h2),
                        if (home.promotionBody.isNotEmpty) ...[
                          const SizedBox(height: 4),
                          Text(
                            home.promotionBody,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: AtgText.small,
                          ),
                        ],
                      ],
                    ),
                  ),
                ),
              if (_loading && home == null)
                const LoadingBlock(padding: 28)
              else if (_live.isEmpty)
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 20, 16, 0),
                  child: SurfaceCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          home == null
                              ? 'Live calls are unavailable'
                              : 'No live calls right now',
                          style: AtgText.h3,
                        ),
                        const SizedBox(height: 3),
                        Text(
                          home == null
                              ? 'We could not reach the ATG server. Pull down to try again.'
                              : 'The floor is quiet. New calls show up here the moment they are posted.',
                          style: AtgText.small,
                        ),
                      ],
                    ),
                  ),
                )
              else ...[
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 20, 16, 0),
                  child: Row(
                    children: [
                      Container(
                        width: 6,
                        height: 6,
                        decoration: const BoxDecoration(
                          color: AtgColors.greenText,
                          shape: BoxShape.circle,
                        ),
                      ),
                      const SizedBox(width: 6),
                      Text(
                        'LIVE NOW',
                        style: AtgText.label.copyWith(
                          color: AtgColors.greenText,
                          letterSpacing: 1.5,
                        ),
                      ),
                    ],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 10, 16, 0),
                  child: SignalCard(
                    signal: _live.first,
                    margin: EdgeInsets.zero,
                    onTap: () => _openSignal(_live.first),
                  ),
                ),
              ],
              const SizedBox(height: 6),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 8),
                child: Row(
                  children: [
                    _QuickAction(
                      icon: Icons.south_west_rounded,
                      label: 'Deposit',
                      onTap: () => Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) => const DepositScreen(),
                        ),
                      ),
                    ),
                    _QuickAction(
                      icon: Icons.north_east_rounded,
                      label: 'Withdraw',
                      onTap: () => Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) => const WithdrawScreen(),
                        ),
                      ),
                    ),
                    _QuickAction(
                      icon: Icons.forum_outlined,
                      label: 'Community',
                      onTap: () => widget.onOpenTab(3),
                    ),
                    _QuickAction(
                      icon: Icons.bolt_outlined,
                      label: 'Calls',
                      onTap: () => widget.onOpenTab(1),
                    ),
                  ],
                ),
              ),
              const Padding(
                padding: EdgeInsets.fromLTRB(16, 14, 16, 0),
                child: SectionLabel('TOOLKIT'),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 10, 16, 0),
                child: Row(
                  children: [
                    Expanded(
                      child: _ToolCard(
                        icon: Icons.calculate_outlined,
                        label: 'Pip calculator',
                        onTap: () => Navigator.of(context).push(
                          MaterialPageRoute(
                            builder: (_) => const PipCalculatorScreen(),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: _ToolCard(
                        icon: Icons.menu_book_outlined,
                        label: 'Glossary',
                        onTap: () => Navigator.of(context).push(
                          MaterialPageRoute(
                            builder: (_) => const GlossaryScreen(),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: _ToolCard(
                        icon: Icons.school_outlined,
                        label: 'Academy',
                        onTap: () => Navigator.of(context).push(
                          MaterialPageRoute(
                            builder: (_) => const AcademyScreen(),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 14),
              TickerStrip(
                tickers: home?.tickers ?? const [],
                onTap: (pair) => _openPair(pair.symbol),
              ),
              if (_fact.isNotEmpty)
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 14, 16, 0),
                  child: SurfaceCard(
                    padding: const EdgeInsets.all(14),
                    onTap: () => showMessage(context, "Today's fact", _fact),
                    child: Row(
                      children: [
                        Container(
                          width: 38,
                          height: 38,
                          decoration: const BoxDecoration(
                            color: AtgColors.primarySoft,
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(
                            Icons.lightbulb_outline_rounded,
                            size: 19,
                            color: AtgColors.gold,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const SectionLabel("TODAY'S FACT"),
                              const SizedBox(height: 3),
                              Text(_fact, style: AtgText.h3),
                            ],
                          ),
                        ),
                        const Icon(
                          Icons.chevron_right_rounded,
                          size: 20,
                          color: AtgColors.muted,
                        ),
                      ],
                    ),
                  ),
                ),
              if (!_inviteDismissed && referralPoints > 0)
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 14, 16, 0),
                  child: SurfaceCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            const Expanded(
                              child: SectionLabel('INVITE AND EARN'),
                            ),
                            InkWell(
                              onTap: () =>
                                  setState(() => _inviteDismissed = true),
                              borderRadius: BorderRadius.circular(16),
                              child: const Padding(
                                padding: EdgeInsets.all(4),
                                child: Icon(
                                  Icons.close_rounded,
                                  size: 16,
                                  color: AtgColors.muted,
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 2),
                        Text(
                          '$referralPoints points per friend',
                          style: AtgText.h2,
                        ),
                        const SizedBox(height: 6),
                        const Text(
                          'Share your code with friends who want to learn. You earn points when they get verified.',
                          style: AtgText.bodySmall,
                        ),
                        const SizedBox(height: 12),
                        OutlineButton(
                          label: 'See how it works',
                          height: 46,
                          onPressed: () => showMessage(
                            context,
                            'Invite and earn',
                            'Share your ATG code with friends who want to learn. When they get verified you earn '
                                '$referralPoints points. Points unlock courses and VIP trials.',
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              const Padding(
                padding: EdgeInsets.fromLTRB(16, 14, 16, 0),
                child: _WeltradeCard(),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 18, 16, 0),
                child: Row(
                  children: [
                    const AtgLogo(size: 20),
                    const SizedBox(width: 8),
                    const Expanded(
                      child: Text(
                        'POWERED BY ATG · ACCURATE TRADING GROUP',
                        style: TextStyle(
                          fontFamily: 'monospace',
                          fontFamilyFallback: ['Menlo', 'Courier New'],
                          fontSize: 9,
                          letterSpacing: 1.1,
                          color: AtgColors.muted,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const Padding(
                padding: EdgeInsets.fromLTRB(16, 12, 16, 16),
                child: Text(
                  'Education and signals only. No financial advice.',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 10.5, color: AtgColors.muted),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _WeltradeCard extends StatefulWidget {
  const _WeltradeCard();

  @override
  State<_WeltradeCard> createState() => _WeltradeCardState();
}

class _WeltradeCardState extends State<_WeltradeCard> {
  static const _link = 'https://track.gowt.me/visit/?bta=46667&brand=weltrade';
  static const _blue = Color(0xFF1B45C8);

  bool _opening = false;

  Future<void> _open() async {
    if (_opening) return;
    setState(() => _opening = true);
    var opened = false;
    try {
      opened = await launchUrl(
        Uri.parse(_link),
        mode: LaunchMode.externalApplication,
      );
    } catch (_) {
      opened = false;
    }
    if (!mounted) return;
    setState(() => _opening = false);
    if (!opened) showToast(context, 'No browser could open the Weltrade page');
  }

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      borderColor: AtgColors.primary,
      padding: const EdgeInsets.fromLTRB(16, 15, 16, 14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SectionLabel('POWERED BY'),
          const SizedBox(height: 8),
          Row(
            children: [
              Container(
                width: 26,
                height: 26,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: _blue,
                  borderRadius: BorderRadius.circular(7),
                ),
                child: const Text(
                  'W',
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w900,
                    fontStyle: FontStyle.italic,
                    color: Colors.white,
                  ),
                ),
              ),
              const SizedBox(width: 8),
              const Text(
                'WELTRADE',
                style: TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w900,
                  fontStyle: FontStyle.italic,
                  letterSpacing: 2.2,
                  color: AtgColors.ink,
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          const _WeltradeStep(
            number: '1',
            text: 'Open a Weltrade account through ATG. It takes about four minutes.',
          ),
          const _WeltradeStep(
            number: '2',
            text: 'Bring your Client ID back here.',
          ),
          const _WeltradeStep(
            number: '3',
            text: 'Fund it from EcoCash, Omari, InnBucks, OneMoney or ZB.',
            last: true,
          ),
          const SizedBox(height: 4),
          SizedBox(
            height: 50,
            width: double.infinity,
            child: FilledButton(
              onPressed: _opening ? null : _open,
              style: FilledButton.styleFrom(
                backgroundColor: AtgColors.primary,
                foregroundColor: AtgColors.ink,
                disabledBackgroundColor: AtgColors.primary.withValues(
                  alpha: 0.45,
                ),
                elevation: 0,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
                textStyle: const TextStyle(
                  fontSize: 14.5,
                  fontWeight: FontWeight.w900,
                ),
              ),
              child: _opening
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: AtgColors.ink,
                      ),
                    )
                  : const Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text('Create a Weltrade account'),
                        SizedBox(width: 6),
                        Icon(Icons.chevron_right_rounded, size: 20),
                      ],
                    ),
            ),
          ),
          const SizedBox(height: 10),
          const Text(
            'Opens Weltrade in your browser. ATG is an official partner and the account stays yours.',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 11, height: 1.4, color: AtgColors.muted),
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
            decoration: BoxDecoration(
              color: AtgColors.redBg,
              borderRadius: BorderRadius.circular(10),
            ),
            child: const Row(
              children: [
                Icon(
                  Icons.warning_amber_rounded,
                  size: 15,
                  color: AtgColors.redText,
                ),
                SizedBox(width: 6),
                Expanded(
                  child: Text(
                    'Accurate Trading Group. Trading carries risk.',
                    style: TextStyle(
                      fontSize: 11,
                      height: 1.3,
                      fontWeight: FontWeight.w700,
                      color: AtgColors.redText,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _WeltradeStep extends StatelessWidget {
  const _WeltradeStep({
    required this.number,
    required this.text,
    this.last = false,
  });

  final String number;
  final String text;
  final bool last;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: last ? 0 : 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 22,
            height: 22,
            alignment: Alignment.center,
            decoration: const BoxDecoration(
              color: AtgColors.greyChip,
              shape: BoxShape.circle,
            ),
            child: Text(
              number,
              style: const TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.w800,
                color: AtgColors.ink,
              ),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              text,
              style: const TextStyle(
                fontSize: 13,
                height: 1.45,
                color: AtgColors.inkSoft,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _QuickAction extends StatelessWidget {
  const _QuickAction({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(14),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 6),
          child: Column(
            children: [
              Container(
                width: 54,
                height: 54,
                decoration: cardDecoration(),
                child: Icon(icon, size: 22, color: AtgColors.ink),
              ),
              const SizedBox(height: 7),
              Text(
                label,
                style: const TextStyle(fontSize: 12, color: AtgColors.ink),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ToolCard extends StatelessWidget {
  const _ToolCard({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(18),
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: cardDecoration(),
        child: Column(
          children: [
            Container(
              width: 38,
              height: 38,
              decoration: const BoxDecoration(
                color: AtgColors.primarySoft,
                shape: BoxShape.circle,
              ),
              child: Icon(icon, size: 19, color: AtgColors.gold),
            ),
            const SizedBox(height: 8),
            Text(
              label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 12, color: AtgColors.ink),
            ),
          ],
        ),
      ),
    );
  }
}
