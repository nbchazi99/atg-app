import 'package:flutter/material.dart';

import '../api.dart';
import '../app_state.dart';
import '../format.dart';
import '../models.dart';
import '../responsive.dart';
import '../theme.dart';
import '../widgets.dart';

class DepositScreen extends StatefulWidget {
  const DepositScreen({super.key});

  @override
  State<DepositScreen> createState() => _DepositScreenState();
}

class _DepositScreenState extends State<DepositScreen> {
  static const _brokers = [
    OptionTile(
      title: 'Weltrade',
      subtitle: 'The broker ATG trades and posts every call against',
      badge: 'W',
      badgeColor: AtgColors.greenText,
      tag: 'Preferred',
    ),
    OptionTile(
      title: 'Deriv',
      subtitle: 'Funded by the nickname on your Deriv account',
      badge: 'D',
      badgeColor: AtgColors.redText,
    ),
    OptionTile(
      title: 'Other',
      subtitle: 'Any other broker. Paid out to a USDT wallet you give us',
      badge: 'O',
    ),
  ];

  static const _wallets = [
    OptionTile(
      title: 'EcoCash',
      subtitle: 'Mobile money · Econet',
      badge: 'EC',
      logo: 'assets/images/wallets/ecocash.png',
    ),
    OptionTile(
      title: 'Omari',
      subtitle: 'Mobile money · OneMoney',
      badge: 'OM',
    ),
    OptionTile(
      title: 'InnBucks',
      subtitle: 'Mobile wallet',
      badge: 'IB',
      logo: 'assets/images/wallets/innbucks.png',
    ),
    OptionTile(
      title: 'OneMoney',
      subtitle: 'Mobile money · NetOne',
      badge: '1M',
      logo: 'assets/images/wallets/onemoney.png',
    ),
    OptionTile(
      title: 'SmileCash',
      subtitle: 'ZB Wallet, confirmed by code',
      badge: 'SC',
      logo: 'assets/images/wallets/smilecash.png',
    ),
  ];

  final _clientId = TextEditingController();
  final _amount = TextEditingController();
  final _mobile = TextEditingController();
  final _payingFrom = TextEditingController();

  int _broker = 0;
  int _wallet = 0;
  Settings? _settings;
  bool _submitting = false;
  String? _clientError;
  String? _amountError;
  String? _mobileError;
  String? _payingError;

  @override
  void initState() {
    super.initState();
    _mobile.text = session.phone;
    _payingFrom.text = session.phone;
    _amount.addListener(() => setState(() {}));
    _loadSettings();
  }

  @override
  void dispose() {
    _clientId.dispose();
    _amount.dispose();
    _mobile.dispose();
    _payingFrom.dispose();
    super.dispose();
  }

  Future<void> _loadSettings() async {
    try {
      final settings = await api.settings();
      if (!mounted) return;
      setState(() => _settings = settings);
    } on ApiException {
      // Defaults below match the server until settings arrive.
    }
  }

  double get _feePct => _settings?.adminFeePct ?? 10;
  double get _min => _settings?.minDeposit ?? 12.50;
  double get _max => _settings?.maxDeposit ?? 454.54;

  double get _enteredAmount => parseAmount(_amount.text);
  double get _fee => (_enteredAmount * _feePct).round() / 100;
  double get _total => ((_enteredAmount + _fee) * 100).round() / 100;

  String get _pctLabel => trimNumber(_feePct);

  String get _clientLabel => switch (_broker) {
    1 => 'Deriv nickname',
    2 => 'USDT wallet address',
    _ => 'Weltrade Client ID',
  };

  String get _clientHint => switch (_broker) {
    1 => 'The nickname shown on your Deriv account, not your email.',
    2 => 'The wallet that should receive your funds. Double-check every character.',
    _ => 'In your Weltrade cabinet, under your name. It is not your MT5 login.',
  };

  Future<void> _submit() async {
    if (_submitting) return;
    setState(() {
      _clientError = null;
      _amountError = null;
      _mobileError = null;
      _payingError = null;
    });

    final amount = _enteredAmount;
    final client = _clientId.text.trim();
    final mobile = _mobile.text.trim();
    final paying = _payingFrom.text.trim();

    var invalid = false;
    if (amount <= 0) {
      _amountError = 'Enter the amount you want to deposit';
      invalid = true;
    } else if (amount < _min) {
      _amountError = 'Minimum deposit is ${money(_min)}';
      invalid = true;
    } else if (amount > _max) {
      _amountError = 'Maximum deposit is ${money(_max)}';
      invalid = true;
    }
    if (client.isEmpty) {
      _clientError = 'Enter your broker account details';
      invalid = true;
    }
    if (mobile.isEmpty) {
      _mobileError = 'Enter the number ATG can reach you on';
      invalid = true;
    }
    if (paying.isEmpty) {
      _payingError = 'Enter the number the money leaves from';
      invalid = true;
    }
    if (invalid) {
      setState(() {});
      return;
    }

    setState(() => _submitting = true);
    try {
      final result = await api.deposit(
        broker: _brokers[_broker].title,
        clientId: client,
        amount: amount,
        wallet: _wallets[_wallet].title,
        mobile: mobile,
      );
      if (!mounted) return;
      setState(() => _submitting = false);
      await showMessage(
        context,
        'Deposit request filed',
        'Pay ${money(result.totalPaid)} to ${result.wallet} on ${result.mobile}. '
            'The administrator will review it and your wallet history will update.\n\n'
            'Reference: DEP-${result.id}',
        action: 'Done',
      );
      if (mounted) Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _submitting = false);
      showMessage(context, 'Could not file the deposit', e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AtgColors.bg,
      body: SafeArea(
        bottom: false,
        child: Column(
          children: [
            const DetailHeader(title: 'Deposit'),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(16, 4, 16, 28),
                children: [
                  SurfaceCard(
                    padding: const EdgeInsets.all(14),
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
                            Icons.south_west_rounded,
                            size: 19,
                            color: AtgColors.gold,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            'Fund your trading account through the ATG desk. A $_pctLabel% admin fee is added to '
                            'what you pay.',
                            style: AtgText.small.copyWith(
                              color: AtgColors.inkSoft,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 20),
                  const SectionLabel('1. CHOOSE THE BROKER'),
                  const SizedBox(height: 10),
                  OptionList(
                    options: _brokers,
                    selected: _broker,
                    onSelected: (index) => setState(() {
                      _broker = index;
                      _clientError = null;
                    }),
                  ),
                  const SizedBox(height: 20),
                  const SectionLabel('2. ACCOUNT DETAILS'),
                  const SizedBox(height: 10),
                  AtgField(
                    controller: _clientId,
                    label: _clientLabel,
                    errorText: _clientError,
                    textCapitalization: TextCapitalization.characters,
                  ),
                  const SizedBox(height: 6),
                  Text(
                    _clientHint,
                    style: AtgText.small.copyWith(fontSize: 11.5),
                  ),
                  const SizedBox(height: 20),
                  const SectionLabel('3. AMOUNT AND WALLET'),
                  const SizedBox(height: 10),
                  AtgField(
                    controller: _amount,
                    label: 'Amount to deposit (USD)',
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                    errorText: _amountError,
                  ),
                  const SizedBox(height: 6),
                  Text(
                    '${money(_min)} to ${money(_max)}, before the $_pctLabel% admin fee.',
                    style: AtgText.small.copyWith(fontSize: 11.5),
                  ),
                  const SizedBox(height: 12),
                  OptionList(
                    options: _wallets,
                    selected: _wallet,
                    onSelected: (index) => setState(() => _wallet = index),
                  ),
                  const SizedBox(height: 20),
                  const SectionLabel('4. CONTACT AND PAYMENT'),
                  const SizedBox(height: 10),
                  AtgField(
                    controller: _mobile,
                    label: 'Mobile number ATG can reach you on',
                    keyboardType: TextInputType.phone,
                    errorText: _mobileError,
                  ),
                  const SizedBox(height: 12),
                  AtgField(
                    controller: _payingFrom,
                    label: 'Number the money leaves from',
                    keyboardType: TextInputType.phone,
                    errorText: _payingError,
                  ),
                  const SizedBox(height: 20),
                  SurfaceCard(
                    padding: const EdgeInsets.all(15),
                    child: PageBody(
                      maxWidth: 720,
                      child: Column(
                        children: [
                          _SummaryRow(
                            label: 'Deposit amount',
                            value: money(_enteredAmount),
                          ),
                          const SizedBox(height: 8),
                          _SummaryRow(
                            label: 'Admin fee ($_pctLabel%)',
                            value: '+ ${money(_fee)}',
                            accent: true,
                          ),
                          const Padding(
                            padding: EdgeInsets.symmetric(vertical: 11),
                            child: Divider(),
                          ),
                          _SummaryRow(
                            label: 'You pay',
                            value: money(_total),
                            strong: true,
                          ),
                          const SizedBox(height: 8),
                          Text(
                            '${money(_total)} leaves your wallet. ${money(_enteredAmount)} reaches your trading account.',
                            style: AtgText.small.copyWith(fontSize: 11.5),
                            textAlign: TextAlign.center,
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  PrimaryButton(
                    label: 'Pay ${money(_total)}',
                    loading: _submitting,
                    onPressed: _submit,
                  ),
                  const SizedBox(height: 10),
                  const Text(
                    'Requests are reviewed by the administrator before the funds move. You will see the decision in '
                    'your wallet history.',
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
    );
  }
}

class _SummaryRow extends StatelessWidget {
  const _SummaryRow({
    required this.label,
    required this.value,
    this.accent = false,
    this.strong = false,
  });

  final String label;
  final String value;
  final bool accent;
  final bool strong;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: Text(label, style: strong ? AtgText.h3 : AtgText.bodySmall),
        ),
        Text(
          value,
          style: TextStyle(
            fontSize: strong ? 19 : 14,
            fontWeight: strong ? FontWeight.w800 : FontWeight.w700,
            color: accent ? AtgColors.gold : AtgColors.ink,
          ),
        ),
      ],
    );
  }
}
