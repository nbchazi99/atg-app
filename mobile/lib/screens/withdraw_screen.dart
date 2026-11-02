import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../api.dart';
import '../app_state.dart';
import '../format.dart';
import '../models.dart';
import '../responsive.dart';
import '../theme.dart';
import '../widgets.dart';

class WithdrawScreen extends StatefulWidget {
  const WithdrawScreen({super.key});

  @override
  State<WithdrawScreen> createState() => _WithdrawScreenState();
}

class _WithdrawScreenState extends State<WithdrawScreen> {
  static const _payouts = [
    OptionTile(
      title: 'EcoCash',
      subtitle: 'Mobile money',
      badge: 'EC',
      logo: 'assets/images/wallets/ecocash.png',
    ),
    OptionTile(
      title: 'SmileCash',
      subtitle: 'Mobile money',
      badge: 'SC',
      tag: 'No fee',
      logo: 'assets/images/wallets/smilecash.png',
    ),
    OptionTile(title: 'Cash', subtitle: 'Collect at the office', badge: 'CA'),
  ];

  static const _sources = [
    OptionTile(
      title: 'Weltrade withdrawal',
      subtitle: 'You withdrew to the ATG agent on Weltrade.',
      badge: 'W',
      badgeColor: AtgColors.greenText,
      tag: 'Common',
    ),
    OptionTile(
      title: 'Deriv withdrawal',
      subtitle: 'You withdrew to the ATG agent on Deriv.',
      badge: 'D',
      badgeColor: AtgColors.redText,
    ),
    OptionTile(
      title: 'Crypto withdrawal',
      subtitle: 'You sent USDT from your own wallet to ATG.',
      badge: 'C',
    ),
  ];

  final _payoutMobile = TextEditingController();
  final _amount = TextEditingController();
  final _contactMobile = TextEditingController();
  final _clientId = TextEditingController();
  final _scroll = ScrollController();

  int _step = 1;
  int _payout = 0;
  int _source = 0;
  Settings? _settings;
  String? _proofUrl;
  String _proofName = 'No screenshot attached yet';
  bool _uploading = false;
  bool _submitting = false;
  WithdrawResult? _result;
  String? _amountError;
  String? _contactError;
  String? _clientError;

  @override
  void initState() {
    super.initState();
    _payoutMobile.text = session.phone;
    _contactMobile.text = session.phone;
    _amount.addListener(() => setState(() {}));
    _loadSettings();
  }

  @override
  void dispose() {
    _payoutMobile.dispose();
    _amount.dispose();
    _contactMobile.dispose();
    _clientId.dispose();
    _scroll.dispose();
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

  double get _feePct => _settings?.withdrawFeePct ?? 7;
  double get _min => _settings?.minWithdraw ?? 12.50;
  double get _enteredAmount => parseAmount(_amount.text);
  double get _fee => (_enteredAmount * _feePct).round() / 100;
  double get _receive => ((_enteredAmount - _fee) * 100).round() / 100;
  String get _pctLabel => trimNumber(_feePct);
  bool get _cashPayout => _payout == 2;

  void _goTo(int step) {
    setState(() => _step = step);
    if (_scroll.hasClients) {
      _scroll.animateTo(
        0,
        duration: const Duration(milliseconds: 200),
        curve: Curves.easeOut,
      );
    }
  }

  bool _validatePayout() {
    if (!_cashPayout && _payoutMobile.text.trim().isEmpty) {
      showToast(context, 'Enter the mobile money number');
      return false;
    }
    return true;
  }

  bool _validateDetails() {
    setState(() {
      _amountError = null;
      _contactError = null;
    });
    final amount = _enteredAmount;
    final contact = _contactMobile.text.trim();
    if (amount <= 0) {
      setState(() => _amountError = 'Enter the amount you want to withdraw');
      return false;
    }
    if (amount < _min) {
      setState(() => _amountError = 'Minimum withdrawal is ${money(_min)}');
      return false;
    }
    if (contact.isEmpty) {
      setState(() => _contactError = 'Enter the number ATG can reach you on');
      return false;
    }
    return true;
  }

  Future<void> _pickProof() async {
    if (_uploading) return;
    try {
      final picked = await ImagePicker().pickImage(
        source: ImageSource.gallery,
        imageQuality: 85,
      );
      if (picked == null || !mounted) return;
      setState(() {
        _uploading = true;
        _proofName = 'Uploading screenshot…';
      });
      final url = await api.uploadImage(picked);
      if (!mounted) return;
      setState(() {
        _proofUrl = url;
        _uploading = false;
        _proofName = 'Screenshot attached. The administrator will check it.';
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _uploading = false;
        _proofName = 'Upload failed. Tap to choose the screenshot again.';
      });
      showMessage(context, 'Could not upload', e.message);
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _uploading = false;
        _proofName = 'Upload failed. Tap to choose the screenshot again.';
      });
      showMessage(
        context,
        'Could not upload',
        'The image could not be read from your gallery.',
      );
    }
  }

  Future<void> _submit() async {
    if (_submitting) return;
    setState(() => _clientError = null);
    if (_clientId.text.trim().isEmpty) {
      setState(() => _clientError = 'Enter your account details');
      return;
    }
    if (_proofUrl == null || _proofUrl!.isEmpty) {
      showToast(context, 'Attach the screenshot of the broker confirmation');
      return;
    }

    final payout = _payouts[_payout].title;
    final mobile = _payoutMobile.text.trim().isEmpty
        ? _contactMobile.text.trim()
        : _payoutMobile.text.trim();
    setState(() => _submitting = true);
    try {
      final result = await api.withdraw(
        payout: payout,
        mobile: mobile,
        amount: _enteredAmount,
        sourceType: _sources[_source].title,
        sourceDetail: _clientId.text.trim(),
        proofUrl: _proofUrl!,
      );
      if (!mounted) return;
      setState(() {
        _result = result;
        _submitting = false;
        _step = 4;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _submitting = false);
      showMessage(context, 'Could not file the withdrawal', e.message);
    }
  }

  String get _clientLabel => switch (_source) {
    1 => 'Deriv nickname',
    2 => 'USDT wallet address you sent from',
    _ => 'Weltrade Client ID',
  };

  String get _clientHint => switch (_source) {
    1 => 'The nickname on your Deriv account, not your email.',
    2 => 'The address the USDT left. Double-check every character.',
    _ => 'In your Weltrade cabinet, under your name. It is not your MT5 login.',
  };

  String get _instructionTitle => switch (_source) {
    1 => 'Withdrawing through Deriv',
    2 => 'Withdrawing through crypto',
    _ => 'Withdrawing through Weltrade',
  };

  String get _instructionBody => switch (_source) {
    1 =>
      '1. In Deriv, start a withdrawal to the ATG agent.\n\n'
          '2. When it completes, screenshot the confirmation.\n\n'
          '3. Attach that screenshot and enter your Deriv nickname.',
    2 =>
      '1. Send USDT to the ATG wallet address the desk gives you.\n\n'
          '2. When the transfer confirms, screenshot it.\n\n'
          '3. Attach that screenshot and paste the sending wallet address.',
    _ =>
      '1. In Weltrade, start a withdrawal to the "ATG" agent.\n\n'
          '2. When it completes, screenshot the confirmation.\n\n'
          '3. Attach that screenshot and enter your Weltrade Client ID.',
  };

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: _step == 1 || _step == 4,
      onPopInvokedWithResult: (didPop, _) {
        if (didPop) return;
        if (_step > 1 && _step < 4) _goTo(_step - 1);
      },
      child: Scaffold(
        backgroundColor: AtgColors.bg,
        body: SafeArea(
          bottom: false,
          child: PageBody(
            maxWidth: 720,
            child: Column(
              children: [
                DetailHeader(
                  title: 'Withdraw',
                  onBack: () {
                    if (_step > 1 && _step < 4) {
                      _goTo(_step - 1);
                    } else {
                      Navigator.of(context).maybePop();
                    }
                  },
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 4, 16, 0),
                  child: _StepBar(step: _step),
                ),
                Expanded(
                  child: ListView(
                    controller: _scroll,
                    padding: const EdgeInsets.fromLTRB(16, 18, 16, 28),
                    children: [
                      if (_step == 1) ..._payoutStep(),
                      if (_step == 2) ..._detailsStep(),
                      if (_step == 3) ..._transferStep(),
                      if (_step == 4) ..._doneStep(),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  List<Widget> _payoutStep() {
    return [
      const SectionLabel('1. WHERE SHOULD THE MONEY GO'),
      const SizedBox(height: 10),
      OptionList(
        options: _payouts,
        selected: _payout,
        onSelected: (index) => setState(() => _payout = index),
      ),
      if (!_cashPayout) ...[
        const SizedBox(height: 16),
        AtgField(
          controller: _payoutMobile,
          label: 'Mobile money number',
          keyboardType: TextInputType.phone,
        ),
        const SizedBox(height: 6),
        const Text(
          'The number that receives the payout. It can differ from your sign-in number.',
          style: TextStyle(fontSize: 11.5, color: AtgColors.muted, height: 1.4),
        ),
      ] else ...[
        const SizedBox(height: 16),
        const SurfaceCard(
          padding: EdgeInsets.all(14),
          child: Text(
            'Collect at the ATG office with your ID. The administrator will call you when the cash is ready.',
            style: AtgText.bodySmall,
          ),
        ),
      ],
      const SizedBox(height: 22),
      PrimaryButton(
        label: 'Continue',
        onPressed: () {
          if (_validatePayout()) _goTo(2);
        },
      ),
    ];
  }

  List<Widget> _detailsStep() {
    return [
      const SectionLabel('2. HOW MUCH'),
      const SizedBox(height: 10),
      AtgField(
        controller: _amount,
        label: 'Amount to withdraw (USD)',
        keyboardType: const TextInputType.numberWithOptions(decimal: true),
        errorText: _amountError,
      ),
      const SizedBox(height: 6),
      Text(
        'Minimum withdrawal is ${money(_min)}.',
        style: AtgText.small.copyWith(fontSize: 11.5),
      ),
      const SizedBox(height: 14),
      AtgField(
        controller: _contactMobile,
        label: 'Mobile number ATG can reach you on',
        keyboardType: TextInputType.phone,
        errorText: _contactError,
      ),
      const SizedBox(height: 18),
      SurfaceCard(
        padding: const EdgeInsets.all(15),
        child: Column(
          children: [
            _SummaryRow(label: 'Withdrawal', value: money(_enteredAmount)),
            const SizedBox(height: 8),
            _SummaryRow(
              label: 'Withdrawal charge ($_pctLabel%)',
              value: '- ${money(_fee)}',
              accent: true,
            ),
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 11),
              child: Divider(),
            ),
            _SummaryRow(
              label: 'You receive',
              value: money(_receive),
              strong: true,
            ),
          ],
        ),
      ),
      const SizedBox(height: 22),
      Row(
        children: [
          Expanded(
            child: OutlineButton(label: 'Back', onPressed: () => _goTo(1)),
          ),
          const SizedBox(width: 10),
          Expanded(
            flex: 2,
            child: PrimaryButton(
              label: 'Continue',
              onPressed: () {
                if (_validateDetails()) _goTo(3);
              },
            ),
          ),
        ],
      ),
    ];
  }

  List<Widget> _transferStep() {
    return [
      const SectionLabel('3. SEND THE FUNDS AND ATTACH PROOF'),
      const SizedBox(height: 10),
      OptionList(
        options: _sources,
        selected: _source,
        onSelected: (index) => setState(() {
          _source = index;
          _clientError = null;
        }),
      ),
      const SizedBox(height: 14),
      SurfaceCard(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(_instructionTitle, style: AtgText.h3),
            const SizedBox(height: 6),
            Text(_instructionBody, style: AtgText.bodySmall),
          ],
        ),
      ),
      const SizedBox(height: 16),
      AtgField(
        controller: _clientId,
        label: _clientLabel,
        errorText: _clientError,
        textCapitalization: TextCapitalization.characters,
      ),
      const SizedBox(height: 6),
      Text(_clientHint, style: AtgText.small.copyWith(fontSize: 11.5)),
      const SizedBox(height: 16),
      InkWell(
        onTap: _pickProof,
        borderRadius: BorderRadius.circular(14),
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: AtgColors.surfaceAlt,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(
              color: _proofUrl == null ? AtgColors.stroke : AtgColors.primary,
              width: 1.2,
            ),
          ),
          child: Row(
            children: [
              Icon(
                _proofUrl == null
                    ? Icons.upload_file_outlined
                    : Icons.check_circle_outline_rounded,
                size: 22,
                color: _proofUrl == null
                    ? AtgColors.muted
                    : AtgColors.greenText,
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('Payment proof', style: AtgText.h3),
                    const SizedBox(height: 2),
                    Text(
                      _proofName,
                      style: AtgText.small.copyWith(fontSize: 11.5),
                    ),
                  ],
                ),
              ),
              if (_uploading)
                const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: AtgColors.gold,
                  ),
                )
              else
                Text(
                  _proofUrl == null ? 'Choose' : 'Change',
                  style: const TextStyle(
                    fontSize: 12,
                    color: AtgColors.gold,
                    fontWeight: FontWeight.w800,
                  ),
                ),
            ],
          ),
        ),
      ),
      const SizedBox(height: 18),
      SurfaceCard(
        padding: const EdgeInsets.all(15),
        child: Column(
          children: [
            _SummaryRow(label: 'Withdrawal', value: money(_enteredAmount)),
            const SizedBox(height: 8),
            _SummaryRow(
              label: 'Withdrawal charge ($_pctLabel%)',
              value: '- ${money(_fee)}',
              accent: true,
            ),
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 11),
              child: Divider(),
            ),
            _SummaryRow(
              label: 'You receive',
              value: money(_receive),
              strong: true,
            ),
          ],
        ),
      ),
      const SizedBox(height: 22),
      Row(
        children: [
          Expanded(
            child: OutlineButton(label: 'Back', onPressed: () => _goTo(2)),
          ),
          const SizedBox(width: 10),
          Expanded(
            flex: 2,
            child: PrimaryButton(
              label: 'Request ${money(_receive)}',
              loading: _submitting,
              onPressed: _submit,
            ),
          ),
        ],
      ),
    ];
  }

  List<Widget> _doneStep() {
    final result = _result;
    return [
      const SizedBox(height: 18),
      Container(
        width: 64,
        height: 64,
        alignment: Alignment.center,
        decoration: const BoxDecoration(
          color: AtgColors.greenBg,
          shape: BoxShape.circle,
        ),
        child: const Icon(
          Icons.check_rounded,
          size: 34,
          color: AtgColors.greenText,
        ),
      ),
      const SizedBox(height: 18),
      const Text(
        'Withdrawal requested',
        textAlign: TextAlign.center,
        style: AtgText.pageTitle,
      ),
      const SizedBox(height: 8),
      Text(
        result == null
            ? 'The request is with the administrator.'
            : 'We will send ${money(result.receive)} to ${result.payout} on ${result.mobile}.',
        textAlign: TextAlign.center,
        style: AtgText.body,
      ),
      const SizedBox(height: 8),
      Text(
        result == null
            ? 'Your wallet history shows the decision.'
            : 'Reference W-${result.id}. The administrator reviews withdrawals manually. '
                  'Your wallet history shows the decision.',
        textAlign: TextAlign.center,
        style: AtgText.small,
      ),
      const SizedBox(height: 24),
      PrimaryButton(
        label: 'Done',
        onPressed: () => Navigator.of(context).pop(true),
      ),
    ];
  }
}

class _StepBar extends StatelessWidget {
  const _StepBar({required this.step});

  final int step;

  static const _labels = ['Payout', 'Amount', 'Proof', 'Done'];

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Step $step of 4', style: AtgText.labelMuted),
        const SizedBox(height: 8),
        Row(
          children: [
            for (var i = 0; i < _labels.length; i++) ...[
              if (i > 0) const SizedBox(width: 6),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _labels[i],
                      style: TextStyle(
                        fontSize: 10.5,
                        fontWeight: FontWeight.w700,
                        color: step >= i + 1 ? AtgColors.gold : AtgColors.muted,
                      ),
                    ),
                    const SizedBox(height: 5),
                    AnimatedContainer(
                      duration: const Duration(milliseconds: 180),
                      height: 3,
                      decoration: BoxDecoration(
                        color: step >= i + 1
                            ? AtgColors.primary
                            : AtgColors.strokeSoft,
                        borderRadius: BorderRadius.circular(2),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ],
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
