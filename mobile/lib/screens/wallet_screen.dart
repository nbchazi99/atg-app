import 'package:flutter/material.dart';

import '../api.dart';
import '../app_state.dart';
import '../models.dart';
import '../realtime.dart';
import '../responsive.dart';
import '../theme.dart';
import '../widgets.dart';
import 'deposit_screen.dart';
import 'profile_screen.dart';
import 'withdraw_screen.dart';

class WalletScreen extends StatefulWidget {
  const WalletScreen({super.key});

  @override
  State<WalletScreen> createState() => _WalletScreenState();
}

class _WalletScreenState extends State<WalletScreen> {
  WalletData? _wallet;
  bool _loading = true;
  VoidCallback? _unbind;

  @override
  void initState() {
    super.initState();
    _load();
    _unbind = realtime.bind({
      AtgScope.wallet(session.id),
      AtgScope.myHome(session.id),
    }, _load);
  }

  @override
  void dispose() {
    _unbind?.call();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final wallet = await api.wallet();
      if (!mounted) return;
      setState(() {
        _wallet = wallet;
        _loading = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _loading = false);
      showToast(context, e.message);
    }
  }

  Future<void> _open(Widget screen) async {
    await Navigator.of(context).push(MaterialPageRoute(builder: (_) => screen));
    if (mounted) await _load();
  }

  void _openProfile() {
    Navigator.of(context)
        .push(MaterialPageRoute(builder: (_) => const ProfileScreen()));
  }

  @override
  Widget build(BuildContext context) {
    final wallet = _wallet;
    final transactions = wallet?.transactions ?? const <WalletTx>[];
    final pending = wallet?.pending ?? 0;

    return SafeArea(
      bottom: false,
      child: PageBody(
        maxWidth: 980,
        child: Column(
          children: [
            TabHeader(
              title: 'Wallet',
              subtitle: 'Money in and out',
              onBell: () => notifyAllCaughtUp(context),
              onAvatar: _openProfile,
            ),
            Expanded(
              child: RefreshIndicator(
                color: AtgColors.primary,
                backgroundColor: AtgColors.surface,
                onRefresh: _load,
                child: ListView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  padding: const EdgeInsets.fromLTRB(16, 4, 16, 100),
                  children: [
                    SurfaceCard(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const SectionLabel('MONEY IN AND OUT'),
                          const SizedBox(height: 8),
                          const Text(
                            'ATG moves money between your mobile wallet and your trading account. It does not hold a '
                            'balance for you, so there is no figure here to spend. Your funds sit with your broker.',
                            style: AtgText.bodySmall,
                          ),
                          const SizedBox(height: 14),
                          Row(
                            children: [
                              Expanded(
                                child: SizedBox(
                                  height: 50,
                                  child: FilledButton(
                                    onPressed: () =>
                                        _open(const DepositScreen()),
                                    style: FilledButton.styleFrom(
                                      backgroundColor: AtgColors.primary,
                                      foregroundColor: AtgColors.ink,
                                      elevation: 0,
                                      shape: RoundedRectangleBorder(
                                        borderRadius: BorderRadius.circular(13),
                                      ),
                                      textStyle: const TextStyle(
                                        fontSize: 14,
                                        fontWeight: FontWeight.w800,
                                      ),
                                    ),
                                    child: const Text('Deposit'),
                                  ),
                                ),
                              ),
                              const SizedBox(width: 8),
                              Expanded(
                                child: OutlineButton(
                                  label: 'Withdraw',
                                  height: 50,
                                  onPressed: () =>
                                      _open(const WithdrawScreen()),
                                ),
                              ),
                            ],
                          ),
                          if (pending > 0) ...[
                            const SizedBox(height: 10),
                            Text(
                              '$pending request${pending == 1 ? '' : 's'} awaiting review',
                              style: const TextStyle(
                                fontSize: 11.5,
                                fontWeight: FontWeight.w600,
                                color: AtgColors.gold,
                              ),
                            ),
                          ],
                        ],
                      ),
                    ),
                    const SizedBox(height: 14),
                    SurfaceCard(
                      padding: const EdgeInsets.all(14),
                      child: Row(
                        children: [
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(session.membershipText, style: AtgText.h3),
                                const SizedBox(height: 2),
                                const Text(
                                  'VIP rooms are included with Deriv VIP or Weltrade VIP.',
                                  style: AtgText.small,
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(width: 8),
                          SizedBox(
                            height: 40,
                            child: OutlinedButton(
                              onPressed: _openProfile,
                              style: OutlinedButton.styleFrom(
                                foregroundColor: AtgColors.ink,
                                backgroundColor: AtgColors.surface,
                                side: const BorderSide(color: AtgColors.stroke),
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(12),
                                ),
                                textStyle: const TextStyle(
                                  fontSize: 12.5,
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                              child: const Text('Manage'),
                            ),
                          ),
                        ],
                      ),
                    ),
                    const Padding(
                      padding: EdgeInsets.fromLTRB(2, 20, 2, 8),
                      child: Text(
                        'Recent activity',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                          color: AtgColors.gold,
                        ),
                      ),
                    ),
                    if (_loading && wallet == null)
                      const LoadingBlock(padding: 24)
                    else if (transactions.isEmpty)
                      const EmptyState(
                        title: 'Nothing yet',
                        message: 'Deposits and withdrawals show up here, from the app and the website.',
                        icon: Icons.receipt_long_outlined,
                      )
                    else
                      SurfaceCard(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 14,
                          vertical: 2,
                        ),
                        child: Column(
                          children: [
                            for (var i = 0; i < transactions.length; i++)
                              TxRow(
                                tx: transactions[i],
                                showDivider: i != transactions.length - 1,
                              ),
                          ],
                        ),
                      ),
                    const SizedBox(height: 10),
                    const Text(
                      'Requests are reviewed by the administrator. Approved deposits reach your broker wallet within '
                      'one working day.',
                      style: TextStyle(
                        fontSize: 11,
                        color: AtgColors.muted,
                        height: 1.45,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
