import 'package:flutter/material.dart';

import '../api.dart';
import '../app_state.dart';
import '../models.dart';
import '../responsive.dart';
import '../theme.dart';
import '../widgets.dart';
import 'pair_detail_screen.dart';
import 'profile_screen.dart';

class MarketsScreen extends StatefulWidget {
  const MarketsScreen({super.key});

  @override
  State<MarketsScreen> createState() => _MarketsScreenState();
}

class _MarketsScreenState extends State<MarketsScreen> {
  static const _categories = ['favourites', 'forex', 'metals', 'crypto'];
  static const _labels = ['Favourites', 'Forex', 'Metals', 'Crypto'];

  final _search = TextEditingController();
  int _category = 1;
  List<MarketPair> _all = const [];
  Set<String> _favourites = {};
  bool _loading = true;
  int _request = 0;
  String _subtitle = 'Loading the desk…';

  @override
  void initState() {
    super.initState();
    _loadFavourites();
    _load();
  }

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    final request = ++_request;
    setState(() => _loading = true);
    try {
      final pairs = await api.markets(_categories[_category]);
      if (!mounted || request != _request) return;
      setState(() {
        _all = pairs;
        _loading = false;
        _subtitle = pairs.length == 1 ? '1 pair' : '${pairs.length} pairs';
      });
    } on ApiException catch (e) {
      if (!mounted || request != _request) return;
      setState(() => _loading = false);
      showToast(context, e.message);
    }
  }

  Future<void> _loadFavourites() async {
    try {
      final pairs = await api.markets('favourites');
      if (!mounted) return;
      setState(() => _favourites = pairs.map((p) => p.symbol).toSet());
    } on ApiException {
      // The star row is a convenience; ignore failures here.
    }
  }

  Future<void> _refresh() async {
    await Future.wait([_load(), _loadFavourites()]);
  }

  Future<void> _toggleFavourite(MarketPair pair) async {
    try {
      await api.toggleFavourite(pair.symbol);
      await _loadFavourites();
      if (!mounted) return;
      if (_category == 0) await _load();
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    }
  }

  List<MarketPair> get _visible {
    final query = _search.text.trim().toLowerCase();
    if (query.isEmpty) return _all;
    return _all
        .where(
          (p) =>
              p.symbol.toLowerCase().contains(query) ||
              p.name.toLowerCase().contains(query),
        )
        .toList();
  }

  @override
  Widget build(BuildContext context) {
    final visible = _visible;
    final categoryName = _categories[_category];

    return SafeArea(
      bottom: false,
      child: PageBody(
        maxWidth: 980,
        child: Column(
          children: [
            TabHeader(
              title: 'Markets',
              subtitle: _subtitle,
              onBell: () => notifyAllCaughtUp(context),
              onAvatar: () => Navigator.of(
                context,
              ).push(MaterialPageRoute(builder: (_) => const ProfileScreen())),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 6, 16, 0),
              child: TextField(
                controller: _search,
                onChanged: (_) => setState(() {}),
                style: const TextStyle(fontSize: 14),
                decoration:
                    atgInputDecoration(
                      hint: categoryName == 'favourites'
                          ? 'Search favourites'
                          : 'Search $categoryName pairs',
                      suffixIcon: _search.text.isEmpty
                          ? null
                          : IconButton(
                              icon: const Icon(Icons.close_rounded, size: 18),
                              color: AtgColors.muted,
                              onPressed: () {
                                _search.clear();
                                setState(() {});
                              },
                            ),
                    ).copyWith(
                      prefixIcon: const Icon(
                        Icons.search_rounded,
                        size: 20,
                        color: AtgColors.muted,
                      ),
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 13,
                      ),
                    ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 6),
              child: AtgChipRow(
                labels: _labels,
                selected: _category,
                onSelected: (index) {
                  if (index == _category) return;
                  setState(() => _category = index);
                  _load();
                },
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
              child: Row(
                children: [
                  SectionLabel(_labels[_category].toUpperCase()),
                  const Spacer(),
                  Text(
                    visible.length == 1 ? '1 pair' : '${visible.length} pairs',
                    style: AtgText.tiny,
                  ),
                ],
              ),
            ),
            Expanded(
              child: RefreshIndicator(
                color: AtgColors.primary,
                backgroundColor: AtgColors.surface,
                onRefresh: _refresh,
                child: _loading && _all.isEmpty
                    ? ListView(
                        physics: const AlwaysScrollableScrollPhysics(),
                        children: const [LoadingBlock()],
                      )
                    : visible.isEmpty
                    ? ListView(
                        physics: const AlwaysScrollableScrollPhysics(),
                        padding: const EdgeInsets.only(top: 24),
                        children: [
                          EmptyState(
                            title: _category == 0
                                ? 'No favourites yet'
                                : 'No pairs found',
                            message: _category == 0
                                ? 'Tap the star beside a pair to keep it here.'
                                : 'Try another search or category.',
                            icon: Icons.star_border_rounded,
                          ),
                        ],
                      )
                    : ListView.builder(
                        physics: const AlwaysScrollableScrollPhysics(),
                        padding: const EdgeInsets.fromLTRB(12, 4, 12, 100),
                        itemCount: visible.length,
                        itemBuilder: (context, index) {
                          final pair = visible[index];
                          return MarketRow(
                            pair: pair,
                            favourite: _favourites.contains(pair.symbol),
                            onTap: () => Navigator.of(context).push(
                              MaterialPageRoute(
                                builder: (_) =>
                                    PairDetailScreen(symbol: pair.symbol),
                              ),
                            ),
                            onFavourite: () => _toggleFavourite(pair),
                          );
                        },
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
