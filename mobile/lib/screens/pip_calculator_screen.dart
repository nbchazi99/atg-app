import 'package:flutter/material.dart';

import '../format.dart';
import '../responsive.dart';
import '../theme.dart';
import '../widgets.dart';

class PipCalculatorScreen extends StatefulWidget {
  const PipCalculatorScreen({super.key});

  @override
  State<PipCalculatorScreen> createState() => _PipCalculatorScreenState();
}

class _PipCalculatorScreenState extends State<PipCalculatorScreen> {
  final _pair = TextEditingController(text: 'EURUSD');
  final _lots = TextEditingController(text: '1');
  final _price = TextEditingController();

  String _value = '—';
  String _detail = 'Enter a pair and lot size, then calculate.';

  @override
  void initState() {
    super.initState();
    _calculate(quiet: true);
  }

  @override
  void dispose() {
    _pair.dispose();
    _lots.dispose();
    _price.dispose();
    super.dispose();
  }

  void _calculate({bool quiet = false}) {
    final symbol = _pair.text.trim().toUpperCase();
    if (symbol.isEmpty) {
      if (!quiet) showToast(context, 'Enter a pair, for example EURUSD');
      return;
    }
    final lotSize = parseAmount(_lots.text, 1);
    if (lotSize <= 0) {
      if (!quiet) showToast(context, 'Enter a lot size above zero');
      return;
    }
    final currentPrice = parseAmount(_price.text);

    final gold = symbol.startsWith('XAU');
    final crypto =
        symbol.startsWith('BTC') ||
        symbol.startsWith('ETH') ||
        symbol.startsWith('SOL');
    final yen = symbol.endsWith('JPY');

    final double pipSize;
    final double contract;
    if (gold) {
      pipSize = 0.1;
      contract = 100;
    } else if (crypto) {
      pipSize = 1;
      contract = 1;
    } else if (yen) {
      pipSize = 0.01;
      contract = 100000;
    } else {
      pipSize = 0.0001;
      contract = 100000;
    }

    if (yen && currentPrice <= 0) {
      setState(() {
        _value = '—';
        _detail = 'Yen pairs are quoted in yen, so enter the current price to convert the pip value into US dollars.';
      });
      return;
    }

    final perPip = yen ? contract * pipSize / currentPrice : contract * pipSize;
    final total = perPip * lotSize;
    final lotText = lotSize == 1
        ? '1 standard lot'
        : '${lotSize.toStringAsFixed(2)} lots';

    setState(() {
      _value = money(total);
      _detail =
          'One pip on $symbol ($lotText) is worth ${money(total)}. '
          'Pip size ${pipSize.toStringAsFixed(4)}'
          '${crypto
              ? ', 1 coin per lot.'
              : gold
              ? ', 100oz per lot.'
              : ', 100,000 units per lot.'}';
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AtgColors.bg,
      body: SafeArea(
        bottom: false,
        child: PageBody(
          maxWidth: 720,
          child: Column(
            children: [
              const DetailHeader(title: 'Pip calculator'),
              Expanded(
                child: ListView(
                  padding: const EdgeInsets.fromLTRB(16, 4, 16, 28),
                  children: [
                    SurfaceCard(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            _value,
                            style: const TextStyle(
                              fontSize: 34,
                              fontWeight: FontWeight.w800,
                              color: AtgColors.ink,
                            ),
                          ),
                          const SizedBox(height: 4),
                          const SectionLabel('VALUE PER PIP'),
                          const SizedBox(height: 8),
                          Text(_detail, style: AtgText.bodySmall),
                        ],
                      ),
                    ),
                    const SizedBox(height: 18),
                    AtgField(
                      controller: _pair,
                      label: 'Pair or symbol',
                      hint: 'EURUSD, USDJPY, XAUUSD, BTCUSD',
                      textCapitalization: TextCapitalization.characters,
                    ),
                    const SizedBox(height: 12),
                    AtgField(
                      controller: _lots,
                      label: 'Lot size',
                      keyboardType: const TextInputType.numberWithOptions(
                        decimal: true,
                      ),
                    ),
                    const SizedBox(height: 12),
                    AtgField(
                      controller: _price,
                      label: 'Current price (yen pairs only)',
                      hint: 'Optional',
                      keyboardType: const TextInputType.numberWithOptions(
                        decimal: true,
                      ),
                    ),
                    const SizedBox(height: 18),
                    PrimaryButton(
                      label: 'Calculate',
                      icon: Icons.calculate_outlined,
                      onPressed: _calculate,
                    ),
                    const SizedBox(height: 14),
                    const Text(
                      'Pip value is an estimate. Your broker may price instruments slightly differently. '
                      'This tool is for education, not advice.',
                      style: TextStyle(
                        fontSize: 11,
                        color: AtgColors.muted,
                        height: 1.45,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
