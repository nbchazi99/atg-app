import 'package:flutter/material.dart';

import '../api.dart';
import '../app_state.dart';
import '../format.dart';
import '../models.dart';
import '../responsive.dart';
import '../theme.dart';
import '../widgets.dart';
import 'signal_detail_screen.dart';

class PairDetailScreen extends StatefulWidget {
  const PairDetailScreen({super.key, required this.symbol});

  final String symbol;

  @override
  State<PairDetailScreen> createState() => _PairDetailScreenState();
}

class _PairDetailScreenState extends State<PairDetailScreen> {
  PairDetail? _detail;
  bool _loading = true;
  bool _toggling = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final detail = await api.pair(widget.symbol);
      if (!mounted) return;
      setState(() {
        _detail = detail;
        _loading = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = e.message;
      });
    }
  }

  Future<void> _toggleFavourite() async {
    if (_toggling) return;
    setState(() => _toggling = true);
    try {
      final favourite = await api.toggleFavourite(widget.symbol);
      if (!mounted) return;
      setState(() {
        final detail = _detail;
        if (detail != null) {
          _detail = PairDetail(
            pair: detail.pair,
            calls: detail.calls,
            favourite: favourite,
          );
        }
        _toggling = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _toggling = false);
      showToast(context, e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    final detail = _detail;

    return Scaffold(
      backgroundColor: AtgColors.bg,
      body: SafeArea(
        bottom: false,
        child: PageBody(
          maxWidth: 900,
          child: Column(
            children: [
              DetailHeader(title: displaySymbol(widget.symbol)),
              Expanded(
                child: _loading && detail == null
                    ? const LoadingBlock()
                    : detail == null
                    ? ListView(
                        padding: const EdgeInsets.only(top: 30),
                        children: [
                          EmptyState(
                            title: 'Could not load this pair',
                            message: _error ?? 'Try again in a moment.',
                            icon: Icons.cloud_off_outlined,
                          ),
                          Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 24),
                            child: OutlineButton(
                              label: 'Try again',
                              onPressed: _load,
                            ),
                          ),
                        ],
                      )
                    : RefreshIndicator(
                        color: AtgColors.primary,
                        backgroundColor: AtgColors.surface,
                        onRefresh: _load,
                        child: ListView(
                          physics: const AlwaysScrollableScrollPhysics(),
                          padding: const EdgeInsets.fromLTRB(16, 4, 16, 28),
                          children: [
                            _buildPriceCard(detail),
                            const SizedBox(height: 14),
                            _buildFavouriteButton(detail),
                            const SizedBox(height: 20),
                            SectionLabel(
                              'ATG CALLS ON ${displaySymbol(widget.symbol)}',
                            ),
                            const SizedBox(height: 10),
                            if (detail.calls.isEmpty)
                              const EmptyState(
                                title: 'No calls yet',
                                message: 'When the desk posts a call on this pair it appears here.',
                                icon: Icons.bolt_outlined,
                              )
                            else
                              for (final signal in detail.calls)
                                SignalCard(
                                  signal: signal,
                                  onTap: () => Navigator.of(context).push(
                                    MaterialPageRoute(
                                      builder: (_) =>
                                          SignalDetailScreen(signal: signal),
                                    ),
                                  ),
                                ),
                          ],
                        ),
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildPriceCard(PairDetail detail) {
    final pair = detail.pair;
    final up = pair.changePct >= 0;
    return SurfaceCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(pair.name, style: AtgText.small),
          const SizedBox(height: 6),
          Text(
            price(pair.price),
            style: const TextStyle(
              fontSize: 38,
              fontWeight: FontWeight.w800,
              color: AtgColors.ink,
              height: 1.1,
            ),
          ),
          const SizedBox(height: 6),
          Row(
            children: [
              AtgPill(
                text: pct(pair.changePct),
                background: up ? AtgColors.greenBg : AtgColors.redBg,
                foreground: up ? AtgColors.greenText : AtgColors.redText,
                fontSize: 11,
              ),
              const SizedBox(width: 6),
              const Text('today', style: AtgText.small),
              const Spacer(),
              if (pair.updatedAt != null && pair.updatedAt!.isNotEmpty)
                Text('Updated ${timeAgo(pair.updatedAt)}', style: AtgText.tiny),
            ],
          ),
          const SizedBox(height: 16),
          const Divider(),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: _Stat(label: 'HIGH', value: price(pair.high)),
              ),
              Expanded(
                child: _Stat(label: 'LOW', value: price(pair.low)),
              ),
              Expanded(
                child: _Stat(label: 'PREV CLOSE', value: price(pair.prevClose)),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildFavouriteButton(PairDetail detail) {
    return OutlineButton(
      label: detail.favourite
          ? 'Remove from favourites'
          : 'Favourite this pair',
      icon: detail.favourite ? Icons.star_rounded : Icons.star_border_rounded,
      onPressed: _toggling ? null : _toggleFavourite,
      foreground: detail.favourite ? AtgColors.gold : AtgColors.ink,
    );
  }
}

class _Stat extends StatelessWidget {
  const _Stat({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: AtgText.label.copyWith(letterSpacing: 1.1)),
        const SizedBox(height: 4),
        Text(
          value,
          style: const TextStyle(
            fontSize: 13.5,
            fontWeight: FontWeight.w700,
            color: AtgColors.ink,
          ),
        ),
      ],
    );
  }
}
