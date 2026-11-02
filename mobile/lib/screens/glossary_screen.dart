import 'package:flutter/material.dart';

import '../responsive.dart';
import '../theme.dart';
import '../widgets.dart';

class GlossaryScreen extends StatefulWidget {
  const GlossaryScreen({super.key});

  @override
  State<GlossaryScreen> createState() => _GlossaryScreenState();
}

class _GlossaryScreenState extends State<GlossaryScreen> {
  static const _terms = <(String, String)>[
    (
      'Pip',
      'The smallest standard move of a currency pair. For most pairs that is 0.0001, for yen pairs 0.01.',
    ),
    (
      'Lot',
      'The standard trade size. One standard lot is 100,000 units of the base currency.',
    ),
    (
      'Leverage',
      'Borrowed exposure from your broker. It multiplies both gains and losses.',
    ),
    (
      'Margin',
      'The money your broker holds while a leveraged position is open.',
    ),
    (
      'Spread',
      'The difference between the bid and the ask. It is a cost you pay on entry.',
    ),
    (
      'Stop loss',
      'An order that closes a trade at a set loss so a bad idea cannot become a disaster.',
    ),
    ('Take profit', 'An order that closes a trade once a target is reached.'),
    ('Entry', 'The price where your position opens.'),
    (
      'Break-even',
      'Moving the stop to your entry so the trade can no longer lose.',
    ),
    (
      'Drawdown',
      "The fall from an account's peak to its lowest point. The number that ends most careers.",
    ),
    (
      'Risk-to-reward',
      'What you stand to make against what you stand to lose. 1:2 means risking \$1 to make \$2.',
    ),
    ('Support', 'A price area where buyers have previously stepped in.'),
    ('Resistance', 'A price area where sellers have previously stepped in.'),
    (
      'Price action',
      'Reading the market from the candles themselves, without indicators.',
    ),
    (
      'Candlestick',
      'One bar showing the open, high, low and close of a period.',
    ),
    (
      'Broker',
      'The firm that holds your trading account and fills your orders.',
    ),
    (
      'Slippage',
      'The difference between the price you wanted and the price you got.',
    ),
    ('Swap', 'The overnight financing charge for holding a position.'),
    (
      'Equity',
      'Your account balance plus or minus the profit and loss on open trades.',
    ),
    (
      'Position sizing',
      'Choosing how many lots to trade so one loss cannot hurt the account.',
    ),
    (
      'ATR',
      'Average True Range. A measure of how much a pair typically moves.',
    ),
    (
      'Wella',
      'An ATG trader who trades on Weltrade, the broker the desk posts calls against.',
    ),
  ];

  final _search = TextEditingController();

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final query = _search.text.trim().toLowerCase();
    final visible = query.isEmpty
        ? _terms
        : _terms
              .where(
                (t) =>
                    t.$1.toLowerCase().contains(query) ||
                    t.$2.toLowerCase().contains(query),
              )
              .toList();

    return Scaffold(
      backgroundColor: AtgColors.bg,
      body: SafeArea(
        bottom: false,
        child: PageBody(
          maxWidth: 720,
          child: Column(
            children: [
              const DetailHeader(title: 'Glossary'),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
                child: TextField(
                  controller: _search,
                  onChanged: (_) => setState(() {}),
                  style: const TextStyle(fontSize: 14),
                  decoration: atgInputDecoration(hint: 'Search a term')
                      .copyWith(
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
              Expanded(
                child: visible.isEmpty
                    ? ListView(
                        padding: const EdgeInsets.only(top: 24),
                        children: const [
                          EmptyState(
                            title: 'No term matches',
                            message: 'Try a shorter word, like pip or lot.',
                            icon: Icons.search_off_rounded,
                          ),
                        ],
                      )
                    : ListView.builder(
                        padding: const EdgeInsets.fromLTRB(16, 4, 16, 28),
                        itemCount: visible.length,
                        itemBuilder: (context, index) {
                          final term = visible[index];
                          return Padding(
                            padding: const EdgeInsets.only(bottom: 10),
                            child: SurfaceCard(
                              padding: const EdgeInsets.all(14),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    term.$1,
                                    style: AtgText.h3.copyWith(fontSize: 14.5),
                                  ),
                                  const SizedBox(height: 5),
                                  Text(term.$2, style: AtgText.bodySmall),
                                ],
                              ),
                            ),
                          );
                        },
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
