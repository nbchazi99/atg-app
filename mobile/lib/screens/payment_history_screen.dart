import 'package:flutter/material.dart';

import '../api.dart';
import '../app_state.dart';
import '../format.dart';
import '../models.dart';
import '../realtime.dart';
import '../theme.dart';
import '../widgets.dart';

/// Receipts and payment history for the signed-in member.
class PaymentHistoryScreen extends StatefulWidget {
  const PaymentHistoryScreen({super.key});

  @override
  State<PaymentHistoryScreen> createState() => _PaymentHistoryScreenState();
}

class _PaymentHistoryScreenState extends State<PaymentHistoryScreen> {
  List<PaymentTx> _payments = const [];
  bool _loading = true;
  String? _error;
  VoidCallback? _unbind;

  @override
  void initState() {
    super.initState();
    _load();
    _unbind = realtime.bind({AtgScope.wallet(session.id), AtgScope.requests}, _load);
  }

  @override
  void dispose() {
    _unbind?.call();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final payments = await api.paymentHistory();
      if (!mounted) return;
      setState(() {
        _payments = payments;
        _loading = false;
        _error = null;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = e.message;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AtgColors.bg,
      appBar: AppBar(
        title: const Text('Payment history', style: AtgText.h3),
        backgroundColor: AtgColors.bg,
        foregroundColor: AtgColors.ink,
        elevation: 0,
      ),
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 720),
            child: _body(),
          ),
        ),
      ),
    );
  }

  Widget _body() {
    if (_loading) return const LoadingBlock(padding: 24);
    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(_error!, textAlign: TextAlign.center, style: AtgText.body),
              const SizedBox(height: 14),
              OutlineButton(label: 'Try again', onPressed: _load),
            ],
          ),
        ),
      );
    }
    if (_payments.isEmpty) {
      return const EmptyState(
        icon: Icons.receipt_long_outlined,
        title: 'No payments yet',
        message: 'Courses and VIP memberships you buy through the payment '
            'gateway appear here with their receipts.',
      );
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.separated(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        itemCount: _payments.length,
        separatorBuilder: (_, _) => const SizedBox(height: 10),
        itemBuilder: (context, index) => _PaymentRow(payment: _payments[index]),
      ),
    );
  }
}

class _PaymentRow extends StatelessWidget {
  const _PaymentRow({required this.payment});

  final PaymentTx payment;

  Color get _statusBg => switch (payment.status) {
    'SUCCESS' => AtgColors.greenBg,
    'FAILED' || 'EXPIRED' => AtgColors.redBg,
    'REFUNDED' => AtgColors.greyChip,
    _ => AtgColors.primarySoft,
  };

  Color get _statusFg => switch (payment.status) {
    'SUCCESS' => AtgColors.greenText,
    'FAILED' || 'EXPIRED' => AtgColors.redText,
    'REFUNDED' => AtgColors.muted,
    _ => AtgColors.gold,
  };

  @override
  Widget build(BuildContext context) {
    return SurfaceCard(
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  payment.serviceName,
                  style: AtgText.h3,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              AtgPill(
                text: payment.statusLabel,
                background: _statusBg,
                foreground: _statusFg,
              ),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Text(money(payment.amount), style: AtgText.h3),
              const SizedBox(width: 8),
              Text(
                timeAgo(payment.createdAt),
                style: AtgText.small.copyWith(color: AtgColors.muted),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            'Reference ${payment.orderId}'
            '${payment.receiptNo != null ? ' · Receipt ${payment.receiptNo}' : ''}',
            style: AtgText.tiny.copyWith(color: AtgColors.muted),
          ),
          if (payment.statusReason != null &&
              payment.statusReason!.isNotEmpty &&
              !payment.isPaid) ...[
            const SizedBox(height: 6),
            Text(
              payment.statusReason!,
              style: AtgText.tiny.copyWith(color: AtgColors.redText),
            ),
          ],
          if (payment.refundNote != null && payment.refundNote!.isNotEmpty) ...[
            const SizedBox(height: 6),
            Text(
              'Refund: ${payment.refundNote}',
              style: AtgText.tiny.copyWith(color: AtgColors.muted),
            ),
          ],
        ],
      ),
    );
  }
}
