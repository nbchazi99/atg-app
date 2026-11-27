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
import 'payment_checkout_screen.dart';
import 'payment_history_screen.dart';
import 'settings_screen.dart';
import 'support_screen.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  List<Purchase> _purchases = const [];
  Settings? _settings;
  AppUser? _me;
  List<WalletTx> _transactions = const [];
  bool _loading = true;
  bool _ordering = false;
  bool _avatarUpdating = false;
  VoidCallback? _unbind;

  Future<void> _changeAvatar() async {
    if (_avatarUpdating) return;
    XFile? image;
    try {
      image = await ImagePicker().pickImage(
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
    if (image == null || !mounted) return;
    setState(() => _avatarUpdating = true);
    try {
      final user = await api.uploadProfileAvatar(image);
      await session.saveUser(user);
      if (mounted) {
        setState(() => _me = user);
        showToast(context, 'Profile photo updated.');
      }
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    } catch (error, stackTrace) {
      debugPrint('Could not upload profile photo: $error\n$stackTrace');
      if (mounted) showToast(context, 'Could not update profile picture');
    } finally {
      if (mounted) setState(() => _avatarUpdating = false);
    }
  }

  Future<void> _removeAvatar() async {
    if (_avatarUpdating || session.avatarUrl == null) return;
    final confirmed = await confirmAction(
      context,
      title: 'Remove profile photo?',
      message: 'Your account will return to its initials avatar.',
      confirmLabel: 'Remove photo',
      cancelLabel: 'Keep photo',
      destructive: true,
    );
    if (!confirmed || !mounted) return;
    setState(() => _avatarUpdating = true);
    try {
      final user = await api.removeProfileAvatar();
      await session.saveUser(user);
      if (mounted) {
        setState(() => _me = user);
        showToast(context, 'Profile photo removed.');
      }
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    } catch (error, stackTrace) {
      debugPrint('Could not remove profile photo: $error\n$stackTrace');
      if (mounted) showToast(context, 'Could not remove profile picture');
    } finally {
      if (mounted) setState(() => _avatarUpdating = false);
    }
  }

  Future<void> _editProfile() async {
    final user = _me;
    if (user == null) return;
    final formKey = GlobalKey<FormState>();
    final name = TextEditingController(text: user.name);
    final username = TextEditingController(text: user.username ?? '');
    final phone = TextEditingController(text: user.phone);
    final country = TextEditingController(text: user.country ?? '');
    final email = TextEditingController(text: user.email);
    final currentPassword = TextEditingController();
    var saving = false;
    var errorMessage = '';
    final changed = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text('Edit profile'),
          content: SizedBox(
            width: 440,
            child: SingleChildScrollView(
              child: Form(
                key: formKey,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    TextFormField(
                      controller: name,
                      textCapitalization: TextCapitalization.words,
                      decoration: const InputDecoration(labelText: 'Full name'),
                      validator: (value) =>
                          value == null || value.trim().isEmpty
                          ? 'Enter your full name.'
                          : value.trim().length > 120
                          ? 'Use 120 characters or fewer.'
                          : null,
                    ),
                    TextFormField(
                      controller: username,
                      textCapitalization: TextCapitalization.none,
                      decoration: const InputDecoration(
                        labelText: 'Username',
                        hintText: '3-32 letters, numbers, . _ or -',
                      ),
                      validator: (value) {
                        final valueTrimmed = value?.trim() ?? '';
                        if (valueTrimmed.isEmpty) return null;
                        if (!RegExp(r'^[A-Za-z0-9._-]{3,32}$')
                            .hasMatch(valueTrimmed)) {
                          return 'Use 3-32 letters, numbers, dots, underscores or hyphens.';
                        }
                        return null;
                      },
                    ),
                    TextFormField(
                      controller: phone,
                      keyboardType: TextInputType.phone,
                      decoration: const InputDecoration(
                        labelText: 'Phone number',
                      ),
                      validator: (value) => (value?.length ?? 0) > 32
                          ? 'Use 32 characters or fewer.'
                          : null,
                    ),
                    TextFormField(
                      controller: country,
                      textCapitalization: TextCapitalization.words,
                      decoration: const InputDecoration(labelText: 'Country'),
                      validator: (value) => (value?.length ?? 0) > 80
                          ? 'Use 80 characters or fewer.'
                          : null,
                    ),
                    TextFormField(
                      controller: email,
                      keyboardType: TextInputType.emailAddress,
                      autofillHints: const [AutofillHints.email],
                      decoration: const InputDecoration(labelText: 'Email'),
                      onChanged: (_) => setDialogState(() {}),
                      validator: (value) {
                        final valueTrimmed = value?.trim() ?? '';
                        if (valueTrimmed.isEmpty) return 'Enter your email.';
                        if (!RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$')
                            .hasMatch(valueTrimmed)) {
                          return 'Enter a valid email address.';
                        }
                        return null;
                      },
                    ),
                    if (email.text.trim().toLowerCase() !=
                        user.email.toLowerCase())
                      TextFormField(
                        controller: currentPassword,
                        obscureText: true,
                        autofillHints: const [AutofillHints.password],
                        decoration: const InputDecoration(
                          labelText:
                              'Current password (required to change email)',
                        ),
                        validator: (value) =>
                            email.text.trim().toLowerCase() !=
                                    user.email.toLowerCase() &&
                                (value == null || value.isEmpty)
                            ? 'Enter your current password.'
                            : null,
                      ),
                    if (errorMessage.isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.only(top: 12),
                        child: Text(
                          errorMessage,
                          style: TextStyle(color: AtgColors.redText),
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: saving ? null : () => Navigator.pop(dialogContext),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: saving
                  ? null
                  : () async {
                      if (!formKey.currentState!.validate()) return;
                      setDialogState(() {
                        saving = true;
                        errorMessage = '';
                      });
                      try {
                        final result = await api.updateProfile(
                          name: name.text.trim(),
                          username: username.text.trim(),
                          phone: phone.text.trim(),
                          country: country.text.trim(),
                          email: email.text.trim(),
                          currentPassword: currentPassword.text.isEmpty
                              ? null
                              : currentPassword.text,
                        );
                        if (result.token.isEmpty) {
                          throw const ApiException(
                            'The server did not return an updated sign-in session.',
                          );
                        }
                        await session.saveAuth(result.token, result.user);
                        if (!mounted || !dialogContext.mounted) return;
                        setState(() => _me = result.user);
                        Navigator.pop(dialogContext, true);
                      } on ApiException catch (e) {
                        if (dialogContext.mounted) {
                          setDialogState(() => errorMessage = e.message);
                        }
                      } catch (error, stackTrace) {
                        debugPrint(
                          'Could not save profile: $error\n$stackTrace',
                        );
                        if (dialogContext.mounted) {
                          setDialogState(
                            () => errorMessage =
                                'Could not save profile. Try again.',
                          );
                        }
                      } finally {
                        if (dialogContext.mounted) {
                          setDialogState(() => saving = false);
                        }
                      }
                    },
              child: saving
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Text('Save changes'),
            ),
          ],
        ),
      ),
    );
    name.dispose();
    username.dispose();
    phone.dispose();
    country.dispose();
    email.dispose();
    currentPassword.dispose();
    if (changed == true && mounted) {
      showToast(context, 'Profile updated.');
    }
  }

  @override
  void initState() {
    super.initState();
    _load();
    _unbind = realtime.bind({
      AtgScope.myHome(session.id),
      AtgScope.wallet(session.id),
      AtgScope.user(session.id),
      AtgScope.rooms,
      AtgScope.courses,
      AtgScope.settings,
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
        api.settings(),
      ]);
      if (!mounted) return;
      final user = results[0] as AppUser;
      await session.saveUser(user);
      if (!mounted) return;
      setState(() {
        _me = user;
        _purchases = results[1] as List<Purchase>;
        _transactions = (results[2] as WalletData).transactions;
        _settings = results[3] as Settings;
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
    final price = type == 'VIP_DERIV'
        ? (_settings?.derivVipPrice ?? 30)
        : (_settings?.weltradeVipPrice ?? 30);
    setState(() => _ordering = true);
    try {
      final paid = await Navigator.of(context).push<bool>(
        MaterialPageRoute(
          builder: (_) => PaymentCheckoutScreen(
            type: type,
            serviceName: '$label membership',
            amount: price,
            prefillPhone: session.phone,
          ),
        ),
      );
      if (!mounted) return;
      setState(() => _ordering = false);
      if (paid == true) {
        showToast(context, '$label unlocked');
        await _load();
      }
    } finally {
      if (mounted && _ordering) setState(() => _ordering = false);
    }
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
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
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
                    SurfaceCard(
                      padding: const EdgeInsets.all(14),
                      onTap: () => Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) => const SettingsScreen(),
                        ),
                      ),
                      child: Row(
                        children: [
                          Icon(
                            Icons.settings_outlined,
                            color: AtgColors.gold,
                            size: 20,
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text('Settings', style: AtgText.h3),
                                SizedBox(height: 2),
                                Text(
                                  'Appearance and app preferences.',
                                  style: AtgText.small,
                                ),
                              ],
                            ),
                          ),
                          Icon(
                            Icons.chevron_right_rounded,
                            color: AtgColors.muted,
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 14),
                    SurfaceCard(
                      padding: const EdgeInsets.all(14),
                      onTap: () => Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) => const SupportScreen(),
                        ),
                      ),
                      child: Row(
                        children: [
                          Icon(
                            Icons.support_agent_rounded,
                            color: AtgColors.gold,
                            size: 20,
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text('Help & Support', style: AtgText.h3),
                                const SizedBox(height: 2),
                                Text(
                                  'Chat with the ATG support engine or follow a support ticket.',
                                  style: AtgText.small,
                                ),
                              ],
                            ),
                          ),
                          Icon(
                            Icons.chevron_right_rounded,
                            color: AtgColors.muted,
                          ),
                        ],
                      ),
                    ),
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
                        child: Row(
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
                                    'Review requests, users, signals, courses and settings.',
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
                    const Padding(
                      padding: EdgeInsets.fromLTRB(2, 18, 2, 8),
                      child: SectionLabel('PAYMENTS'),
                    ),
                    SurfaceCard(
                      padding: const EdgeInsets.all(14),
                      onTap: () => Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) => const PaymentHistoryScreen(),
                        ),
                      ),
                      child: Row(
                        children: [
                          Icon(
                            Icons.receipt_long_outlined,
                            color: AtgColors.gold,
                            size: 20,
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text('Payment history', style: AtgText.h3),
                                const SizedBox(height: 2),
                                Text(
                                  'Receipts for courses and VIP purchases.',
                                  style: AtgText.small,
                                ),
                              ],
                            ),
                          ),
                          Icon(
                            Icons.chevron_right_rounded,
                            color: AtgColors.muted,
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 22),
                    OutlineButton(
                      label: 'Sign out',
                      icon: Icons.logout_rounded,
                      foreground: AtgColors.redText,
                      onPressed: _logout,
                    ),
                    const SizedBox(height: 14),
                    Text(
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
          Tooltip(
            message: 'Change profile photo',
            child: GestureDetector(
              onTap: _avatarUpdating ? null : _changeAvatar,
              child: Stack(
                alignment: Alignment.bottomRight,
                children: [
                  AvatarCircle(
                    text: session.initialsText,
                    size: 60,
                    imageUrl: session.avatarUrl,
                  ),
                  Container(
                    width: 22,
                    height: 22,
                    decoration: BoxDecoration(
                      color: AtgColors.gold,
                      shape: BoxShape.circle,
                      border: Border.all(
                        color: Theme.of(context).colorScheme.surface,
                        width: 2,
                      ),
                    ),
                    child: Icon(
                      Icons.camera_alt_rounded,
                      size: 12,
                      color: AtgColors.primary,
                    ),
                  ),
                  if (_avatarUpdating)
                    Positioned.fill(
                      child: ClipOval(
                        child: ColoredBox(
                          color: Colors.black45,
                          child: Center(
                            child: SizedBox(
                              width: 24,
                              height: 24,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: AtgColors.gold,
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                ],
              ),
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(session.name, style: AtgText.h1),
                const SizedBox(height: 2),
                if ((session.username).isNotEmpty)
                  Text('@${session.username}', style: AtgText.small),
                Text(session.email, style: AtgText.small),
                if (session.phone.isNotEmpty)
                  Text(
                    session.phone,
                    style: AtgText.small.copyWith(fontSize: 11.5),
                  ),
                if (session.country.isNotEmpty)
                  Text(
                    session.country,
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
          IconButton(
            tooltip: 'Remove profile photo',
            onPressed: _avatarUpdating || session.avatarUrl == null
                ? null
                : _removeAvatar,
            icon: Icon(Icons.delete_outline_rounded, color: AtgColors.redText),
          ),
          IconButton(
            tooltip: 'Edit profile',
            onPressed: _editProfile,
            icon: Icon(Icons.edit_outlined, color: AtgColors.gold),
          ),
        ],
      ),
    );
  }

  String? _expiryText() {
    final raw = _me?.membershipExpiresAt;
    if (raw == null || raw.isEmpty) return null;
    final dt = DateTime.tryParse(raw)?.toLocal();
    if (dt == null) return null;
    const months = [
      'Jan',
      'Feb',
      'Mar',
      'Apr',
      'May',
      'Jun',
      'Jul',
      'Aug',
      'Sep',
      'Oct',
      'Nov',
      'Dec',
    ];
    final date = '${dt.day} ${months[dt.month - 1]} ${dt.year}';
    return _me!.membershipExpired
        ? 'VIP access expired on $date'
        : 'VIP access until $date';
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
          Text(
            'VIP rooms are included with Deriv VIP or Weltrade VIP. Pay '
            '${money(_settings?.derivVipPrice ?? 30)} securely through the payment '
            'gateway and the rooms open automatically once the payment is confirmed.',
            style: AtgText.bodySmall,
          ),
          if (_expiryText() != null) ...[
            const SizedBox(height: 8),
            Row(
              children: [
                Icon(Icons.schedule_rounded, size: 14, color: AtgColors.muted),
                const SizedBox(width: 6),
                Text(
                  _expiryText()!,
                  style: AtgText.small.copyWith(color: AtgColors.muted),
                ),
              ],
            ),
          ],
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: _MembershipOption(
                  title: 'Deriv VIP',
                  badge: 'D',
                  active: derivActive,
                  busy: _ordering,
                  onTap: derivActive ? null : () => _join('VIP_DERIV'),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: _MembershipOption(
                  title: 'Weltrade VIP',
                  badge: 'W',
                  active: weltradeActive,
                  busy: _ordering,
                  onTap: weltradeActive ? null : () => _join('VIP_WELTRADE'),
                ),
              ),
            ],
          ),
          if (_ordering) ...[
            const SizedBox(height: 12),
            Row(
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
                Text('Opening checkout…', style: AtgText.small),
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
                  decoration: BoxDecoration(
                    color: AtgColors.primarySoft,
                    shape: BoxShape.circle,
                  ),
                  child: Text(
                    badge,
                    style: TextStyle(
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
                    style: TextStyle(
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
                style: TextStyle(
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
