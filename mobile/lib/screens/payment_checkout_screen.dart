import 'dart:async';

import 'package:flutter/material.dart';

import '../api.dart';
import '../app_state.dart';
import '../format.dart';
import '../models.dart';
import '../theme.dart';
import '../widgets.dart';

/// Checkout for every paid service: creates a Dischub order, which pushes an
/// EcoCash payment prompt to the customer's phone. The customer approves the
/// prompt on their handset and this screen polls until the backend confirms
/// the payment — the customer never leaves the app (non-redirect integration,
/// see ecocash.md).
///
/// The screen never decides anything itself — `api.paymentStatus` hits our
/// backend, which re-verifies with the gateway before reporting success.
class PaymentCheckoutScreen extends StatefulWidget {
  const PaymentCheckoutScreen({
    super.key,
    required this.type,
    this.courseId,
    required this.serviceName,
    required this.amount,
    this.prefillPhone,
  });

  final String type;
  final int? courseId;
  final String serviceName;
  final double amount;
  final String? prefillPhone;

  @override
  State<PaymentCheckoutScreen> createState() => _PaymentCheckoutScreenState();
}

class _PaymentCheckoutScreenState extends State<PaymentCheckoutScreen> {
  final _phone = TextEditingController();

  String? _orderId;
  PaymentTx? _order;
  String _ecocashStatus = '';
  String? _error;
  bool _creating = false;
  bool _checking = false;
  Timer? _poll;

  @override
  void initState() {
    super.initState();
    _phone.text = widget.prefillPhone ?? '';
  }

  @override
  void dispose() {
    _poll?.cancel();
    _phone.dispose();
    super.dispose();
  }

  bool get _settled =>
      _order != null &&
      (_order!.isPaid ||
          _order!.status == 'FAILED' ||
          _order!.status == 'EXPIRED');

  Future<void> _startPayment() async {
    final phone = _phone.text.trim();
    if (phone.isEmpty) {
      setState(() => _error = 'Enter the mobile number that will receive the payment prompt');
      return;
    }
    setState(() {
      _creating = true;
      _error = null;
    });
    try {
      final result = await api.createPaymentIntent(
        type: widget.type,
        courseId: widget.courseId,
        senderPhone: phone,
      );
      if (!mounted) return;
      setState(() {
        _creating = false;
        _orderId = result.order.orderId;
        _order = result.order;
        _ecocashStatus = result.ecocashStatus;
      });
      _beginPolling();
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _creating = false;
        _error = e.message;
      });
    }
  }

  void _beginPolling() {
    _poll?.cancel();
    _poll = Timer.periodic(const Duration(seconds: 3), (_) => _check(quiet: true));
    _check(quiet: true);
  }

  Future<void> _check({bool quiet = false}) async {
    final orderId = _orderId;
    if (orderId == null || _checking) return;
    if (_settled) {
      _poll?.cancel();
      return;
    }
    setState(() => _checking = true);
    try {
      final order = await api.paymentStatus(orderId);
      if (!mounted) return;
      setState(() => _order = order);
      if (_settled) _poll?.cancel();
    } on ApiException catch (e) {
      if (!mounted) return;
      if (!quiet) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _checking = false);
    }
  }

  void _restart() {
    _poll?.cancel();
    setState(() {
      _orderId = null;
      _order = null;
      _ecocashStatus = '';
      _error = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AtgColors.bg,
      appBar: AppBar(
        title: const Text('Checkout', style: AtgText.h3),
        backgroundColor: AtgColors.bg,
        foregroundColor: AtgColors.ink,
        elevation: 0,
      ),
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 560),
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
              child: _buildBody(),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildBody() {
    if (_order != null && _order!.isPaid) return _SuccessView(order: _order!);
    if (_order != null && (_order!.status == 'FAILED' || _order!.status == 'EXPIRED')) {
      return _FailedView(order: _order!, onRetry: _restart);
    }
    if (_orderId != null) {
      return _WaitingView(
        order: _order,
        ecocashStatus: _ecocashStatus,
        phone: _phone.text.trim(),
        checking: _checking,
        onCheckNow: () => _check(),
      );
    }
    return _FormView(
      serviceName: widget.serviceName,
      amount: widget.amount,
      phone: _phone,
      creating: _creating,
      error: _error,
      onPay: _startPayment,
    );
  }
}

class _FormView extends StatelessWidget {
  const _FormView({
    required this.serviceName,
    required this.amount,
    required this.phone,
    required this.creating,
    required this.error,
    required this.onPay,
  });

  final String serviceName;
  final double amount;
  final TextEditingController phone;
  final bool creating;
  final String? error;
  final VoidCallback onPay;

  @override
  Widget build(BuildContext context) {
    return ListView(
      shrinkWrap: true,
      children: [
        SurfaceCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(serviceName, style: AtgText.h3),
              const SizedBox(height: 6),
              Text('Total due', style: AtgText.small.copyWith(color: AtgColors.muted)),
              const SizedBox(height: 4),
              Text(money(amount), style: AtgText.h1),
              const SizedBox(height: 16),
              const Divider(height: 1),
              const SizedBox(height: 16),
              Text(
                'Press send and an EcoCash payment prompt will arrive on the '
                'number below. Approve it on your phone — you stay in this app, '
                'and the purchase unlocks automatically once the payment is '
                'confirmed.',
                style: AtgText.bodySmall.copyWith(color: AtgColors.inkSoft),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        AtgField(
          label: 'EcoCash number for the payment prompt',
          controller: phone,
          hint: '0770000000',
          keyboardType: TextInputType.phone,
        ),
        if (error != null) ...[
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AtgColors.redBg,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Text(error!, style: AtgText.small.copyWith(color: AtgColors.redText)),
          ),
        ],
        const SizedBox(height: 20),
        PrimaryButton(
          label: 'Send prompt · Pay ${money(amount)}',
          onPressed: onPay,
          loading: creating,
          icon: Icons.phone_iphone,
        ),
        const SizedBox(height: 10),
        Center(
          child: Text(
            'Payments are processed by Dischub',
            style: AtgText.tiny.copyWith(color: AtgColors.muted),
          ),
        ),
      ],
    );
  }
}

class _WaitingView extends StatelessWidget {
  const _WaitingView({
    required this.order,
    required this.ecocashStatus,
    required this.phone,
    required this.checking,
    required this.onCheckNow,
  });

  final PaymentTx? order;
  final String ecocashStatus;
  final String phone;
  final bool checking;
  final VoidCallback onCheckNow;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const SizedBox(
            width: 46,
            height: 46,
            child: CircularProgressIndicator(strokeWidth: 3, color: AtgColors.primary),
          ),
          const SizedBox(height: 22),
          Text('Approve the prompt on your phone', style: AtgText.h3),
          const SizedBox(height: 8),
          Text(
            phone.isEmpty
                ? 'An EcoCash payment prompt has been sent. Check your phone, '
                      'enter your EcoCash PIN to authorise the payment, and this '
                      'screen will update automatically.'
                : 'An EcoCash payment prompt was sent to $phone. Check that '
                      'phone, enter your EcoCash PIN to authorise the payment, '
                      'and this screen will update automatically.',
            textAlign: TextAlign.center,
            style: AtgText.bodySmall.copyWith(color: AtgColors.inkSoft),
          ),
          if (ecocashStatus.isNotEmpty) ...[
            const SizedBox(height: 14),
            AtgPill(
              text: ecocashStatus.toUpperCase(),
              background: AtgColors.primarySoft,
              foreground: AtgColors.gold,
              fontSize: 10,
            ),
          ],
          if (order != null) ...[
            const SizedBox(height: 14),
            Text(
              'Reference ${order!.orderId} · ${money(order!.amount)}',
              style: AtgText.small.copyWith(color: AtgColors.muted),
            ),
          ],
          const SizedBox(height: 26),
          SizedBox(
            width: 280,
            child: PrimaryButton(
              label: "I've approved it — check now",
              onPressed: onCheckNow,
              loading: checking,
            ),
          ),
        ],
      ),
    );
  }
}

class _SuccessView extends StatelessWidget {
  const _SuccessView({required this.order});

  final PaymentTx order;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            width: 76,
            height: 76,
            decoration: BoxDecoration(
              color: AtgColors.greenBg,
              shape: BoxShape.circle,
            ),
            child: Icon(Icons.check, size: 42, color: AtgColors.greenText),
          ),
          const SizedBox(height: 22),
          Text('Payment confirmed', style: AtgText.h2),
          const SizedBox(height: 8),
          Text(
            '${order.serviceName} is now unlocked.',
            textAlign: TextAlign.center,
            style: AtgText.body.copyWith(color: AtgColors.inkSoft),
          ),
          const SizedBox(height: 18),
          SurfaceCard(
            child: Column(
              children: [
                _row('Amount', money(order.amount)),
                _row('Reference', order.orderId),
                _row('Receipt', order.receiptNo ?? '—'),
                if (order.paidAt != null) _row('Paid', timeAgo(order.paidAt)),
              ],
            ),
          ),
          const SizedBox(height: 24),
          SizedBox(
            width: 260,
            child: PrimaryButton(
              label: 'Done',
              onPressed: () => Navigator.of(context).pop(true),
            ),
          ),
        ],
      ),
    );
  }

  Widget _row(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 7),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: AtgText.small.copyWith(color: AtgColors.muted)),
          Text(value, style: AtgText.small.copyWith(fontWeight: FontWeight.w700)),
        ],
      ),
    );
  }
}

class _FailedView extends StatelessWidget {
  const _FailedView({required this.order, required this.onRetry});

  final PaymentTx order;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    final reason = order.statusReason;
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            width: 76,
            height: 76,
            decoration: BoxDecoration(
              color: AtgColors.redBg,
              shape: BoxShape.circle,
            ),
            child: Icon(Icons.close, size: 42, color: AtgColors.redText),
          ),
          const SizedBox(height: 22),
          Text(
            order.status == 'EXPIRED' ? 'Payment expired' : 'Payment failed',
            style: AtgText.h2,
          ),
          const SizedBox(height: 8),
          Text(
            reason == null || reason.isEmpty
                ? 'The payment was not completed. You can try again.'
                : reason,
            textAlign: TextAlign.center,
            style: AtgText.body.copyWith(color: AtgColors.inkSoft),
          ),
          const SizedBox(height: 8),
          Text(
            'Reference ${order.orderId}',
            style: AtgText.small.copyWith(color: AtgColors.muted),
          ),
          const SizedBox(height: 26),
          SizedBox(
            width: 260,
            child: PrimaryButton(label: 'Try again', onPressed: onRetry),
          ),
        ],
      ),
    );
  }
}

/// Landing page after Dischub redirects the browser back to the app
/// (`/payment?order=PMT…`). Shows live, backend-verified status.
class PaymentResultScreen extends StatefulWidget {
  const PaymentResultScreen({super.key, required this.orderId});

  final String orderId;

  @override
  State<PaymentResultScreen> createState() => _PaymentResultScreenState();
}

class _PaymentResultScreenState extends State<PaymentResultScreen> {
  PaymentTx? _order;
  String? _error;
  Timer? _poll;
  bool _checking = false;

  @override
  void initState() {
    super.initState();
    _check();
    _poll = Timer.periodic(const Duration(seconds: 3), (_) => _check(quiet: true));
  }

  @override
  void dispose() {
    _poll?.cancel();
    super.dispose();
  }

  bool get _settled =>
      _order != null &&
      (_order!.isPaid ||
          _order!.status == 'FAILED' ||
          _order!.status == 'EXPIRED' ||
          _order!.status == 'REFUNDED');

  Future<void> _check({bool quiet = false}) async {
    if (_checking) return;
    if (_settled) {
      _poll?.cancel();
      return;
    }
    setState(() => _checking = true);
    try {
      final order = await api.paymentStatus(widget.orderId);
      if (!mounted) return;
      setState(() {
        _order = order;
        _error = null;
      });
      if (_settled) _poll?.cancel();
    } on ApiException catch (e) {
      if (!mounted) return;
      if (!quiet) setState(() => _error = e.message);
      if (e.status == 401) _poll?.cancel();
    } finally {
      if (mounted) setState(() => _checking = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AtgColors.bg,
      appBar: AppBar(
        title: const Text('Payment status', style: AtgText.h3),
        backgroundColor: AtgColors.bg,
        foregroundColor: AtgColors.ink,
        elevation: 0,
      ),
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 560),
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
              child: _body(),
            ),
          ),
        ),
      ),
    );
  }

  Widget _body() {
    if (_order != null && (_order!.isPaid || _order!.status == 'REFUNDED')) {
      return Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Expanded(child: _SuccessView(order: _order!)),
          TextButton(
            onPressed: () => Navigator.of(context).popUntil((r) => r.isFirst),
            child: Text(
              'Back to ATG',
              style: AtgText.small.copyWith(color: AtgColors.goldDark),
            ),
          ),
        ],
      );
    }
    if (_order != null && (_order!.status == 'FAILED' || _order!.status == 'EXPIRED')) {
      return Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Expanded(child: _FailedView(order: _order!, onRetry: () {})),
          TextButton(
            onPressed: () => Navigator.of(context).popUntil((r) => r.isFirst),
            child: Text(
              'Back to ATG',
              style: AtgText.small.copyWith(color: AtgColors.goldDark),
            ),
          ),
        ],
      );
    }
    if (_error != null && _order == null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(_error!, textAlign: TextAlign.center, style: AtgText.body),
            const SizedBox(height: 16),
            OutlineButton(label: 'Check again', onPressed: _check),
          ],
        ),
      );
    }
    return _WaitingView(
      order: _order,
      ecocashStatus: '',
      phone: '',
      checking: _checking,
      onCheckNow: () => _check(),
    );
  }
}
