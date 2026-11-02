import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../api.dart';
import '../app_state.dart';
import '../format.dart';
import '../models.dart';
import '../realtime.dart';
import '../responsive.dart';
import '../theme.dart';
import '../widgets.dart';
import 'admin_screen.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  List<Purchase> _purchases = const [];
  List<WalletTx> _transactions = const [];
  bool _loading = true;
  bool _ordering = false;
  String? _pendingType;
  VoidCallback? _unbind;

  @override
  void initState() {
    super.initState();
    _load();
    _unbind = realtime.bind({
      AtgScope.myHome(session.id),
      AtgScope.wallet(session.id),
      AtgScope.rooms,
      AtgScope.courses,
    }, _load);
  }

  @override
  void dispose() {
    _unbind?.call();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final results = await Future.wait([
        api.me(),
        api.purchases(),
        api.wallet(),
      ]);
      if (!mounted) return;
      final user = results[0] as AppUser;
      await session.saveUser(user);
      if (!mounted) return;
      setState(() {
        _purchases = results[1] as List<Purchase>;
        _transactions = (results[2] as WalletData).transactions;
        _loading = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _loading = false);
      showToast(context, e.message);
    }
  }

  Future<void> _join(String type) async {
    if (_ordering) return;
    final label = type == 'VIP_DERIV' ? 'Deriv VIP' : 'Weltrade VIP';
    final proceed = await confirmAction(
      context,
      title: 'Join $label',
      message:
          'Pay \$30.00 to the ATG desk, then attach a screenshot of the payment. '
          'The administrator reviews proofs manually and unlocks the room once approved.',
      confirmLabel: 'Choose screenshot',
    );
    if (!proceed || !mounted) return;

    XFile? proof;
    try {
      proof = await ImagePicker().pickImage(
        source: ImageSource.gallery,
        imageQuality: 85,
      );
    } catch (_) {
      if (mounted) {
        showMessage(
          context,
          'Could not open the gallery',
          'Allow photo access for ATG and try again.',
        );
      }
      return;
    }
    if (proof == null || !mounted) return;

    setState(() {
      _ordering = true;
      _pendingType = type;
    });
    try {
      final url = await api.uploadImage(proof);
      final purchase = await api.purchase(type, proofUrl: url);
      if (!mounted) return;
      setState(() {
        _ordering = false;
        _pendingType = null;
      });
      await showMessage(
        context,
        'Request filed',
        'Reference P-${purchase.id}. $label unlocks here once the administrator approves the proof.',
        action: 'Done',
      );
      if (mounted) await _load();
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _ordering = false;
        _pendingType = null;
      });
      showMessage(context, 'Could not file the purchase', e.message);
    }
  }

  Future<void> _editServer() async {
    await showServerDialog(context);
    if (mounted) setState(() {});
  }

  Future<void> _logout() async {
    final confirmed = await confirmAction(
      context,
      title: 'Sign out?',
      message: 'Your session on this phone ends. Courses, purchases and requests stay on your account.',
      confirmLabel: 'Sign out',
      cancelLabel: 'Stay',
      destructive: true,
    );
    if (!confirmed) return;
    await session.logout();
    if (!mounted) return;
    Navigator.of(context).popUntil((route) => route.isFirst);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AtgColors.bg,
      body: SafeArea(
        bottom: false,
        child: PageBody(
          maxWidth: 900,
          child: Column(
            children: [
              const DetailHeader(title: 'Profile'),
              Expanded(
                child: ListView(
                  padding: const EdgeInsets.fromLTRB(16, 4, 16, 28),
                  children: [
                    _identityCard(),
                    const SizedBox(height: 14),
                    _membershipCard(),
                    if (session.isAdmin) ...[
                      const SizedBox(height: 14),
                      SurfaceCard(
                        padding: const EdgeInsets.all(14),
                        onTap: () => Navigator.of(context).push(
                          MaterialPageRoute(
                            builder: (_) => const AdminScreen(),
                          ),
                        ),
                        child: const Row(
                          children: [
                            Icon(
                              Icons.shield_outlined,
                              size: 20,
                              color: AtgColors.gold,
                            ),
                            SizedBox(width: 10),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text('Admin dashboard', style: AtgText.h3),
                                  SizedBox(height: 2),
                                  Text(
                                    'Review requests, users, calls, courses and settings.',
                                    style: AtgText.small,
                                  ),
                                ],
                              ),
                            ),
                            Icon(
                              Icons.chevron_right_rounded,
                              size: 20,
                              color: AtgColors.muted,
                            ),
                          ],
                        ),
                      ),
                    ],
                    const Padding(
                      padding: EdgeInsets.fromLTRB(2, 22, 2, 8),
                      child: SectionLabel('PURCHASES'),
                    ),
                    if (_loading && _purchases.isEmpty)
                      const LoadingBlock(padding: 16)
                    else if (_purchases.isEmpty)
                      const SurfaceCard(
                        padding: EdgeInsets.all(14),
                        child: Text('No purchases yet.', style: AtgText.small),
                      )
                    else
                      SurfaceCard(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 14,
                          vertical: 4,
                        ),
                        child: Column(
                          children: [
                            for (var i = 0; i < _purchases.length; i++)
                              _PurchaseRow(
                                purchase: _purchases[i],
                                showDivider: i != _purchases.length - 1,
                              ),
                          ],
                        ),
                      ),
                    const Padding(
                      padding: EdgeInsets.fromLTRB(2, 22, 2, 8),
                      child: SectionLabel('RECENT ACTIVITY'),
                    ),
                    if (_transactions.isEmpty)
                      const SurfaceCard(
                        padding: EdgeInsets.all(14),
                        child: Text(
                          'Deposits and withdrawals show up here.',
                          style: AtgText.small,
                        ),
                      )
                    else
                      SurfaceCard(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 14,
                          vertical: 2,
                        ),
                        child: Column(
                          children: [
                            for (var i = 0; i < _transactions.length; i++)
                              TxRow(
                                tx: _transactions[i],
                                showDivider: i != _transactions.length - 1,
                              ),
                          ],
                        ),
                      ),
                    const SizedBox(height: 22),
                    SurfaceCard(
                      padding: const EdgeInsets.all(14),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              const Icon(
                                Icons.dns_outlined,
                                size: 18,
                                color: AtgColors.muted,
                              ),
                              const SizedBox(width: 8),
                              const Expanded(
                                child: Text(
                                  'Server address',
                                  style: AtgText.h3,
                                ),
                              ),
                              TextButton(
                                onPressed: _editServer,
                                child: const Text(
                                  'Change',
                                  style: TextStyle(
                                    color: AtgColors.gold,
                                    fontWeight: FontWeight.w800,
                                    fontSize: 12.5,
                                  ),
                                ),
                              ),
                            ],
                          ),
                          Text(
                            session.server,
                            style: AtgText.small.copyWith(fontSize: 11.5),
                          ),
                          const SizedBox(height: 4),
                          const Text(
                            '10.0.2.2 for the Android emulator, 127.0.0.1 for the iOS simulator, or your computer\'s '
                            'LAN IP on a real phone.',
                            style: TextStyle(
                              fontSize: 10.5,
                              color: AtgColors.muted,
                              height: 1.4,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 14),
                    OutlineButton(
                      label: 'Sign out',
                      icon: Icons.logout_rounded,
                      foreground: AtgColors.redText,
                      onPressed: _logout,
                    ),
                    const SizedBox(height: 14),
                    const Text(
                      'Education and signals only. No financial advice.',
                      textAlign: TextAlign.center,
                      style: TextStyle(fontSize: 10.5, color: AtgColors.muted),
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

  Widget _identityCard() {
    return SurfaceCard(
      child: Row(
        children: [
          AvatarCircle(text: session.initialsText, size: 60),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(session.name, style: AtgText.h1),
                const SizedBox(height: 2),
                Text(session.email, style: AtgText.small),
                if (session.phone.isNotEmpty)
                  Text(
                    session.phone,
                    style: AtgText.small.copyWith(fontSize: 11.5),
                  ),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 6,
                  runSpacing: 6,
                  children: [
                    AtgPill(
                      text: session.isAdmin ? 'ADMIN' : 'LEARNER',
                      background: session.isAdmin
                          ? AtgColors.primarySoft
                          : AtgColors.greyChip,
                      foreground: session.isAdmin
                          ? AtgColors.gold
                          : AtgColors.muted,
                    ),
                    AtgPill(
                      text: session.membershipText.toUpperCase(),
                      background: session.membership == 'NONE'
                          ? AtgColors.greyChip
                          : AtgColors.greenBg,
                      foreground: session.membership == 'NONE'
                          ? AtgColors.muted
                          : AtgColors.greenText,
                    ),
                    AtgPill(
                      text: '${session.points} POINTS',
                      background: AtgColors.primarySoft,
                      foreground: AtgColors.gold,
                      icon: Icons.star_rounded,
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _membershipCard() {
    final derivActive = session.membership == 'DERIV_VIP';
    final weltradeActive = session.membership == 'WELTRADE_VIP';

    return SurfaceCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Expanded(child: SectionLabel('VIP MEMBERSHIP')),
              AtgPill(
                text: session.membershipText.toUpperCase(),
                background: session.membership == 'NONE'
                    ? AtgColors.greyChip
                    : AtgColors.greenBg,
                foreground: session.membership == 'NONE'
                    ? AtgColors.muted
                    : AtgColors.greenText,
              ),
            ],
          ),
          const SizedBox(height: 8),
          const Text(
            'VIP rooms are included with Deriv VIP or Weltrade VIP. Pay \$30.00 to the desk, attach the proof, and '
            'the rooms open after the administrator approves it.',
            style: AtgText.bodySmall,
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: _MembershipOption(
                  title: 'Deriv VIP',
                  badge: 'D',
                  active: derivActive,
                  busy: _pendingType == 'VIP_DERIV',
                  onTap: derivActive ? null : () => _join('VIP_DERIV'),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: _MembershipOption(
                  title: 'Weltrade VIP',
                  badge: 'W',
                  active: weltradeActive,
                  busy: _pendingType == 'VIP_WELTRADE',
                  onTap: weltradeActive ? null : () => _join('VIP_WELTRADE'),
                ),
              ),
            ],
          ),
          if (_ordering) ...[
            const SizedBox(height: 12),
            const Row(
              children: [
                SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: AtgColors.gold,
                  ),
                ),
                SizedBox(width: 8),
                Text('Filing your proof…', style: AtgText.small),
              ],
            ),
          ],
        ],
      ),
    );
  }
}

class _MembershipOption extends StatelessWidget {
  const _MembershipOption({
    required this.title,
    required this.badge,
    required this.active,
    required this.busy,
    required this.onTap,
  });

  final String title;
  final String badge;
  final bool active;
  final bool busy;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: busy ? null : onTap,
      borderRadius: BorderRadius.circular(14),
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: active ? AtgColors.surfaceAlt : AtgColors.surface,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(
            color: active ? AtgColors.greenText : AtgColors.stroke,
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  width: 28,
                  height: 28,
                  alignment: Alignment.center,
                  decoration: const BoxDecoration(
                    color: AtgColors.primarySoft,
                    shape: BoxShape.circle,
                  ),
                  child: Text(
                    badge,
                    style: const TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w800,
                      color: AtgColors.gold,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 12.5,
                      fontWeight: FontWeight.w700,
                      color: AtgColors.ink,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              busy
                  ? 'Uploading…'
                  : (active ? 'Active membership' : 'Pay proof and join'),
              style: TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.w700,
                color: active ? AtgColors.greenText : AtgColors.gold,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _PurchaseRow extends StatelessWidget {
  const _PurchaseRow({required this.purchase, required this.showDivider});

  final Purchase purchase;
  final bool showDivider;

  static String _label(String type) {
    switch (type) {
      case 'COURSE':
        return 'Course';
      case 'VIP_DERIV':
        return 'Deriv VIP';
      case 'VIP_WELTRADE':
        return 'Weltrade VIP';
      default:
        return type;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 11),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(_label(purchase.type), style: AtgText.h3),
                    const SizedBox(height: 2),
                    Text(
                      timeAgo(purchase.createdAt),
                      style: AtgText.small.copyWith(fontSize: 11.5),
                    ),
                  ],
                ),
              ),
              Text(
                money(purchase.amount),
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w800,
                  color: AtgColors.ink,
                ),
              ),
              const SizedBox(width: 8),
              StatusPill(purchase.status),
            ],
          ),
        ),
        if (showDivider) const Divider(height: 1),
      ],
    );
  }
}
