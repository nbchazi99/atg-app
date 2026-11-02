import 'package:flutter/material.dart';

import '../api.dart';
import '../app_state.dart';
import '../models.dart';
import '../realtime.dart';
import '../responsive.dart';
import '../theme.dart';
import '../widgets.dart';
import 'pair_detail_screen.dart';
import 'profile_screen.dart';
import 'signal_detail_screen.dart';

class SignalsScreen extends StatefulWidget {
  const SignalsScreen({super.key});

  @override
  State<SignalsScreen> createState() => _SignalsScreenState();
}

class _SignalsScreenState extends State<SignalsScreen> {
  static const _filters = ['LIVE', 'OPEN', 'INNER', 'CLOSED'];
  static const _labels = ['Live', 'Open', 'Inner', 'Closed'];

  int _filter = 0;
  List<Signal> _items = const [];
  List<MarketPair> _tickers = const [];
  String _subtitle = 'Loading the floor…';
  bool _loading = true;
  int _request = 0;
  VoidCallback? _unbind;

  @override
  void initState() {
    super.initState();
    _load();
    _loadTickers();
    _unbind = realtime.bind(
      {AtgScope.signals, AtgScope.home, AtgScope.settings},
      () {
        _load();
        _loadTickers();
      },
    );
  }

  @override
  void dispose() {
    _unbind?.call();
    super.dispose();
  }

  Future<void> _loadTickers() async {
    try {
      final home = await api.home();
      if (!mounted) return;
      setState(() => _tickers = home.tickers);
    } on ApiException {
      // Tickers are decorative; the signal list reports connection problems.
    }
  }

  Future<void> _load() async {
    final request = ++_request;
    setState(() => _loading = true);
    try {
      final items = await api.signals(status: _filters[_filter]);
      if (!mounted || request != _request) return;
      setState(() {
        _items = items;
        _loading = false;
        _subtitle = items.length == 1 ? '1 call' : '${items.length} calls';
      });
    } on ApiException catch (e) {
      if (!mounted || request != _request) return;
      setState(() => _loading = false);
      showToast(context, e.message);
    }
  }

  void _openProfile() {
    Navigator.of(context)
        .push(MaterialPageRoute(builder: (_) => const ProfileScreen()));
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      bottom: false,
      child: PageBody(
        maxWidth: 1080,
        child: Column(
          children: [
            TabHeader(
              title: 'Signals',
              subtitle: _subtitle,
              onBell: () => notifyAllCaughtUp(context),
              onAvatar: _openProfile,
            ),
            TickerStrip(
              tickers: _tickers,
              onTap: (pair) => Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => PairDetailScreen(symbol: pair.symbol),
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 10, 16, 4),
              child: AtgChipRow(
                labels: _labels,
                selected: _filter,
                onSelected: (index) {
                  if (index == _filter) return;
                  setState(() => _filter = index);
                  _load();
                },
              ),
            ),
            Expanded(
              child: RefreshIndicator(
                color: AtgColors.primary,
                backgroundColor: AtgColors.surface,
                onRefresh: _load,
                child: _loading && _items.isEmpty
                    ? ListView(
                        physics: const AlwaysScrollableScrollPhysics(),
                        children: const [LoadingBlock()],
                      )
                    : _items.isEmpty
                    ? ListView(
                        physics: const AlwaysScrollableScrollPhysics(),
                        padding: const EdgeInsets.only(top: 30),
                        children: const [
                          EmptyState(
                            title: 'Nothing here yet',
                            message: 'Calls appear here as soon as the floor posts them.',
                            icon: Icons.bolt_outlined,
                          ),
                        ],
                      )
                    : _SignalGrid(
                        items: _items,
                        onOpen: (signal) => Navigator.of(context).push(
                          MaterialPageRoute(
                            builder: (_) => SignalDetailScreen(signal: signal),
                          ),
                        ),
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SignalGrid extends StatelessWidget {
  const _SignalGrid({required this.items, required this.onOpen});

  final List<Signal> items;
  final ValueChanged<Signal> onOpen;

  @override
  Widget build(BuildContext context) {
    final twoUp = AtgLayout.of(context).columns > 1;
    if (!twoUp) {
      return ListView.builder(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(16, 10, 16, 100),
        itemCount: items.length,
        itemBuilder: (context, index) =>
            SignalCard(signal: items[index], onTap: () => onOpen(items[index])),
      );
    }
    return ListView.builder(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(16, 10, 16, 100),
      itemCount: (items.length / 2).ceil(),
      itemBuilder: (context, row) {
        final left = row * 2;
        final right = left + 1;
        return Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: SignalCard(
                  signal: items[left],
                  margin: EdgeInsets.zero,
                  onTap: () => onOpen(items[left]),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: right < items.length
                    ? SignalCard(
                        signal: items[right],
                        margin: EdgeInsets.zero,
                        onTap: () => onOpen(items[right]),
                      )
                    : const SizedBox.shrink(),
              ),
            ],
          ),
        );
      },
    );
  }
}
