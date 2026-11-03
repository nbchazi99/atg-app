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

class AdminScreen extends StatefulWidget {
  const AdminScreen({super.key});

  @override
  State<AdminScreen> createState() => _AdminScreenState();
}

class _AdminScreenState extends State<AdminScreen> {
  static const _labels = [
    'Requests',
    'Users',
    'Signals',
    'Courses',
    'Settings',
    'Account',
  ];

  final _adminFee = TextEditingController();
  final _withdrawFee = TextEditingController();
  final _minDeposit = TextEditingController();
  final _maxDeposit = TextEditingController();
  final _minWithdraw = TextEditingController();
  final _fact = TextEditingController();
  final _promoTitle = TextEditingController();
  final _promoBody = TextEditingController();
  final _currentPassword = TextEditingController();
  final _newEmail = TextEditingController();
  final _newPassword = TextEditingController();
  final _confirmPassword = TextEditingController();

  int _tab = 0;
  bool _loading = true;
  AdminRequests? _requests;
  List<AppUser> _users = const [];
  List<Signal> _signals = const [];
  List<Course> _courses = const [];
  bool _savingSettings = false;
  bool _savingAccount = false;
  VoidCallback? _unbind;

  @override
  void initState() {
    super.initState();
    _loadTab();
    _unbind = realtime.bind(
      {AtgScope.requests, AtgScope.users, AtgScope.signals, AtgScope.courses},
      () {
        if (!mounted || _savingSettings || _tab == 4 || _tab == 5) return;
        _loadTab();
      },
    );
  }

  @override
  void dispose() {
    _unbind?.call();
    _adminFee.dispose();
    _withdrawFee.dispose();
    _minDeposit.dispose();
    _maxDeposit.dispose();
    _minWithdraw.dispose();
    _fact.dispose();
    _promoTitle.dispose();
    _promoBody.dispose();
    _currentPassword.dispose();
    _newEmail.dispose();
    _newPassword.dispose();
    _confirmPassword.dispose();
    super.dispose();
  }

  void _select(int index) {
    if (index == _tab) return;
    setState(() => _tab = index);
    _loadTab();
  }

  Future<void> _loadTab() async {
    setState(() => _loading = true);
    try {
      switch (_tab) {
        case 1:
          final users = await api.adminUsers();
          if (!mounted) return;
          setState(() {
            _users = users;
            _loading = false;
          });
          break;
        case 2:
          final signals = await api.adminSignals();
          if (!mounted) return;
          setState(() {
            _signals = signals;
            _loading = false;
          });
          break;
        case 3:
          final rows = await api.courses();
          if (!mounted) return;
          setState(() {
            _courses = rows.map((r) => r.course).toList();
            _loading = false;
          });
          break;
        case 4:
          final settings = await api.adminSettings();
          if (!mounted) return;
          _fillSettings(settings);
          setState(() => _loading = false);
          break;
        case 5:
          if (!mounted) return;
          setState(() => _loading = false);
          break;
        default:
          final requests = await api.adminRequests();
          if (!mounted) return;
          setState(() {
            _requests = requests;
            _loading = false;
          });
      }
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _loading = false);
      showToast(context, e.message);
    }
  }

  void _fillSettings(Settings s) {
    _adminFee.text = trimNumber(s.adminFeePct);
    _withdrawFee.text = trimNumber(s.withdrawFeePct);
    _minDeposit.text = trimNumber(s.minDeposit);
    _maxDeposit.text = trimNumber(s.maxDeposit);
    _minWithdraw.text = trimNumber(s.minWithdraw);
    _fact.text = s.fact;
    _promoTitle.text = s.promotionTitle;
    _promoBody.text = s.promotionBody;
  }

  List<_AdminRequest> get _requestRows {
    final requests = _requests;
    if (requests == null) return const [];
    final rows = <_AdminRequest>[];
    for (final d in requests.deposits) {
      rows.add(
        _AdminRequest(
          kind: 'DEPOSIT',
          id: d.id,
          title: 'Deposit · ${_safe(d.broker)}',
          meta:
              'Client ${_safe(d.clientId)} · ${_safe(d.wallet)} · ${_safe(d.mobile)}',
          amount: d.totalPaid,
          status: d.status,
        ),
      );
    }
    for (final w in requests.withdrawals) {
      rows.add(
        _AdminRequest(
          kind: 'WITHDRAWAL',
          id: w.id,
          title: 'Withdrawal · ${_safe(w.payout)}',
          meta:
              '${_safe(w.sourceType)} · ${_safe(w.mobile)} · receive ${money(w.receive)}',
          amount: w.amount,
          status: w.status,
        ),
      );
    }
    for (final p in requests.purchases) {
      rows.add(
        _AdminRequest(
          kind: 'PURCHASE',
          id: p.id,
          title: 'Purchase · ${_purchaseLabel(p.type)}',
          meta:
              'Reference P-${p.id}${p.courseId == null ? '' : ' · Course ${p.courseId}'}',
          amount: p.amount,
          status: p.status,
        ),
      );
    }
    return rows;
  }

  static String _safe(String value) => value.trim().isEmpty ? '—' : value;

  static String _purchaseLabel(String type) {
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

  Future<void> _decide(_AdminRequest row, bool approve) async {
    final note = TextEditingController();
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        backgroundColor: AtgColors.surface,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Text(
          '${approve ? 'Approve' : 'Reject'} ${row.title}',
          style: AtgText.h1.copyWith(fontSize: 16),
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('${money(row.amount)} · ${row.status}', style: AtgText.small),
            const SizedBox(height: 12),
            TextField(
              controller: note,
              style: const TextStyle(fontSize: 14),
              decoration: atgInputDecoration(
                hint: 'Note for the user (optional)',
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: const Text(
              'Cancel',
              style: TextStyle(
                color: AtgColors.muted,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: Text(
              approve ? 'Approve' : 'Reject',
              style: TextStyle(
                color: approve ? AtgColors.greenText : AtgColors.redText,
                fontWeight: FontWeight.w800,
              ),
            ),
          ),
        ],
      ),
    );
    final noteText = note.text.trim();
    note.dispose();
    if (confirmed != true || !mounted) return;

    try {
      await api.decide(row.kind, row.id, approve, noteText);
      if (!mounted) return;
      showToast(context, 'Request updated');
      await _loadTab();
    } on ApiException catch (e) {
      if (!mounted) return;
      showMessage(context, 'Could not update the request', e.message);
    }
  }

  Future<void> _manageMembership(AppUser user) async {
    var selected = switch (user.membership) {
      'DERIV_VIP' => 1,
      'WELTRADE_VIP' => 2,
      _ => 0,
    };
    final saved = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (dialogContext, setDialogState) => AlertDialog(
          backgroundColor: AtgColors.surface,
          surfaceTintColor: Colors.transparent,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
          ),
          title: Text(user.name, style: AtgText.h1),
          content: SingleChildScrollView(
            child: OptionList(
              options: const [
                OptionTile(
                  title: 'No membership',
                  subtitle: 'Standard rooms only',
                  badge: '–',
                ),
                OptionTile(
                  title: 'Deriv VIP',
                  subtitle: 'Unlocks the Deriv VIP room',
                  badge: 'D',
                ),
                OptionTile(
                  title: 'Weltrade VIP',
                  subtitle: 'Unlocks the Weltrade VIP room',
                  badge: 'W',
                ),
              ],
              selected: selected,
              onSelected: (index) => setDialogState(() => selected = index),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(false),
              child: const Text(
                'Cancel',
                style: TextStyle(
                  color: AtgColors.muted,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(true),
              child: const Text(
                'Save',
                style: TextStyle(
                  color: AtgColors.gold,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ),
          ],
        ),
      ),
    );
    if (saved != true || !mounted) return;
    const values = ['NONE', 'DERIV_VIP', 'WELTRADE_VIP'];
    try {
      await api.setMembership(user.id, values[selected]);
      if (!mounted) return;
      showToast(context, 'Membership updated');
      await _loadTab();
    } on ApiException catch (e) {
      if (!mounted) return;
      showMessage(context, 'Could not update the membership', e.message);
    }
  }

  Future<void> _addSignal() async {
    final signal = await showModalBottomSheet<Signal>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AtgColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(22)),
      ),
      builder: (_) => const _SignalFormSheet(),
    );
    if (signal == null || !mounted) return;
    try {
      await api.createSignal(signal);
      if (!mounted) return;
      showToast(context, 'Signal published');
      await _loadTab();
    } on ApiException catch (e) {
      if (!mounted) return;
      showMessage(context, 'Could not publish the signal', e.message);
    }
  }

  Future<void> _deleteSignal(Signal signal) async {
    final confirmed = await confirmAction(
      context,
      title: 'Delete ${signal.title}?',
      message: 'The call disappears for every trader on the floor.',
      confirmLabel: 'Delete',
      cancelLabel: 'Keep',
      destructive: true,
    );
    if (!confirmed || !mounted) return;
    try {
      await api.deleteSignal(signal.id);
      if (!mounted) return;
      showToast(context, 'Signal deleted');
      await _loadTab();
    } on ApiException catch (e) {
      if (!mounted) return;
      showMessage(context, 'Could not delete the signal', e.message);
    }
  }

  Future<void> _addCourse() async {
    final draft = await showModalBottomSheet<_CourseDraft>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AtgColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(22)),
      ),
      builder: (_) => const _CourseFormSheet(),
    );
    if (draft == null || !mounted) return;
    try {
      await api.createCourse(
        title: draft.title,
        subtitle: draft.subtitle,
        description: draft.description,
        price: draft.price,
        level: draft.level,
        imageUrl: draft.imageUrl,
        lessons: draft.lessons,
      );
      if (!mounted) return;
      showToast(context, 'Course created');
      await _loadTab();
    } on ApiException catch (e) {
      if (!mounted) return;
      showMessage(context, 'Could not create the course', e.message);
    }
  }

  Future<void> _deleteCourse(Course course) async {
    final confirmed = await confirmAction(
      context,
      title: 'Delete ${course.title}?',
      message: 'Lessons and course access are removed. Enrolled traders lose the course.',
      confirmLabel: 'Delete',
      cancelLabel: 'Keep',
      destructive: true,
    );
    if (!confirmed || !mounted) return;
    try {
      await api.deleteCourse(course.id);
      if (!mounted) return;
      showToast(context, 'Course deleted');
      await _loadTab();
    } on ApiException catch (e) {
      if (!mounted) return;
      showMessage(context, 'Could not delete the course', e.message);
    }
  }

  Future<void> _saveSettings() async {
    if (_savingSettings) return;
    setState(() => _savingSettings = true);
    try {
      await api.updateSettings({
        'adminFeePct': parseAmount(_adminFee.text, 10),
        'withdrawFeePct': parseAmount(_withdrawFee.text, 7),
        'minDeposit': parseAmount(_minDeposit.text, 12.5),
        'maxDeposit': parseAmount(_maxDeposit.text, 454.54),
        'minWithdraw': parseAmount(_minWithdraw.text, 12.5),
        'fact': _fact.text.trim(),
        'promotionTitle': _promoTitle.text.trim(),
        'promotionBody': _promoBody.text.trim(),
      });
      if (!mounted) return;
      setState(() => _savingSettings = false);
      showToast(context, 'Settings saved');
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _savingSettings = false);
      showMessage(context, 'Could not save the settings', e.message);
    }
  }

  Future<void> _saveAccount() async {
    if (_savingAccount) return;
    final current = _currentPassword.text;
    if (current.isEmpty) {
      showToast(context, 'Enter your current password');
      return;
    }
    final newEmail = _newEmail.text.trim();
    final newPass = _newPassword.text;
    if (newEmail.isNotEmpty && !newEmail.contains('@')) {
      showToast(context, 'That does not look like an email');
      return;
    }
    if (newPass.isNotEmpty && newPass.length < 6) {
      showToast(context, 'New password must be at least 6 characters');
      return;
    }
    if (newPass != _confirmPassword.text) {
      showToast(context, 'New password and confirmation do not match');
      return;
    }
    setState(() => _savingAccount = true);
    try {
      final result = await api.updateAdminAccount(
        currentPassword: current,
        email: newEmail,
        password: newPass,
      );
      if (!mounted) return;
      await session.saveAuth(result.token, result.user);
      if (!mounted) return;
      setState(() {
        _savingAccount = false;
        _currentPassword.clear();
        _newEmail.clear();
        _newPassword.clear();
        _confirmPassword.clear();
      });
      showToast(context, 'Login credentials updated');
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _savingAccount = false);
      showMessage(context, 'Could not update the credentials', e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AtgColors.bg,
      body: SafeArea(
        bottom: false,
        child: PageBody(
          maxWidth: 1080,
          child: Column(
            children: [
              const DetailHeader(
                title: 'Admin',
                trailing: LiveDot(showLabel: true),
              ),
              SizedBox(
                height: 44,
                child: ListView.separated(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  itemCount: _labels.length,
                  separatorBuilder: (_, _) => const SizedBox(width: 6),
                  itemBuilder: (context, index) => SizedBox(
                    width: 92,
                    child: AtgChip(
                      label: _labels[index],
                      selected: _tab == index,
                      height: 38,
                      onTap: () => _select(index),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 10),
              Expanded(
                child: _loading
                    ? const LoadingBlock()
                    : switch (_tab) {
                        1 => _usersView(),
                        2 => _signalsView(),
                        3 => _coursesView(),
                        4 => _settingsView(),
                        5 => _accountView(),
                        _ => _requestsView(),
                      },
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _requestsView() {
    final rows = _requestRows;
    if (rows.isEmpty) {
      return ListView(
        padding: const EdgeInsets.only(top: 20),
        children: const [
          EmptyState(
            title: 'No requests',
            message:
                'Deposits, withdrawals and purchases land here for review.',
            icon: Icons.inbox_outlined,
          ),
        ],
      );
    }
    return RefreshIndicator(
      color: AtgColors.primary,
      backgroundColor: AtgColors.surface,
      onRefresh: _loadTab,
      child: ListView.builder(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(16, 4, 16, 28),
        itemCount: rows.length,
        itemBuilder: (context, index) {
          final row = rows[index];
          return Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: SurfaceCard(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          row.title,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: AtgText.h3,
                        ),
                      ),
                      const SizedBox(width: 8),
                      StatusPill(row.status),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    row.meta,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: AtgText.small,
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Text(
                        money(row.amount),
                        style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w800,
                          color: AtgColors.ink,
                        ),
                      ),
                      const Spacer(),
                      if (row.status == 'PENDING') ...[
                        SizedBox(
                          height: 34,
                          child: OutlinedButton(
                            onPressed: () => _decide(row, false),
                            style: OutlinedButton.styleFrom(
                              foregroundColor: AtgColors.redText,
                              side: const BorderSide(color: AtgColors.stroke),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(10),
                              ),
                              textStyle: const TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            child: const Text('Reject'),
                          ),
                        ),
                        const SizedBox(width: 8),
                        SizedBox(
                          height: 34,
                          child: FilledButton(
                            onPressed: () => _decide(row, true),
                            style: FilledButton.styleFrom(
                              backgroundColor: AtgColors.primary,
                              foregroundColor: AtgColors.ink,
                              elevation: 0,
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(10),
                              ),
                              textStyle: const TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                            child: const Text('Approve'),
                          ),
                        ),
                      ],
                    ],
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _usersView() {
    if (_users.isEmpty) {
      return ListView(
        padding: const EdgeInsets.only(top: 20),
        children: const [
          EmptyState(
            title: 'No users',
            message: 'Traders appear here once they register.',
            icon: Icons.people_outline_rounded,
          ),
        ],
      );
    }
    return ListView.builder(
      padding: const EdgeInsets.fromLTRB(16, 4, 16, 28),
      itemCount: _users.length,
      itemBuilder: (context, index) {
        final user = _users[index];
        return Padding(
          padding: const EdgeInsets.only(bottom: 10),
          child: SurfaceCard(
            padding: const EdgeInsets.all(13),
            onTap: () => _manageMembership(user),
            child: Row(
              children: [
                AvatarCircle(text: initials(user.name), size: 40),
                const SizedBox(width: 11),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Flexible(
                            child: Text(
                              user.name,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: AtgText.h3,
                            ),
                          ),
                          if (user.isAdmin) ...[
                            const SizedBox(width: 6),
                            const AtgPill(
                              text: 'ADMIN',
                              background: AtgColors.primarySoft,
                              foreground: AtgColors.gold,
                              fontSize: 9,
                            ),
                          ],
                        ],
                      ),
                      const SizedBox(height: 2),
                      Text(
                        user.email,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: AtgText.small.copyWith(fontSize: 11.5),
                      ),
                      const SizedBox(height: 5),
                      Text(
                        membershipLabel(user.membership),
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.w700,
                          color: user.membership == 'NONE'
                              ? AtgColors.muted
                              : AtgColors.greenText,
                        ),
                      ),
                    ],
                  ),
                ),
                const Icon(
                  Icons.chevron_right_rounded,
                  size: 20,
                  color: AtgColors.muted,
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _signalsView() {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 10),
          child: Row(
            children: [
              Text(
                _signals.isEmpty ? 'No calls yet' : '${_signals.length} calls',
                style: AtgText.small,
              ),
              const Spacer(),
              SizedBox(
                height: 36,
                child: FilledButton(
                  onPressed: _addSignal,
                  style: FilledButton.styleFrom(
                    backgroundColor: AtgColors.primary,
                    foregroundColor: AtgColors.ink,
                    elevation: 0,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(10),
                    ),
                    textStyle: const TextStyle(
                      fontSize: 12.5,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  child: const Text('New signal'),
                ),
              ),
            ],
          ),
        ),
        Expanded(
          child: _signals.isEmpty
              ? ListView(
                  padding: const EdgeInsets.only(top: 20),
                  children: const [
                    EmptyState(
                      title: 'No calls',
                      message: 'Publish the first call for the floor.',
                      icon: Icons.bolt_outlined,
                    ),
                  ],
                )
              : ListView.builder(
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 28),
                  itemCount: _signals.length,
                  itemBuilder: (context, index) {
                    final signal = _signals[index];
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: SurfaceCard(
                        padding: const EdgeInsets.all(13),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Expanded(
                                  child: Text(
                                    signal.title,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: AtgText.h3,
                                  ),
                                ),
                                const SizedBox(width: 6),
                                SidePill(signal.side, fontSize: 9),
                                const SizedBox(width: 5),
                                StatusPill(signal.status, fontSize: 9),
                                IconButton(
                                  onPressed: () => _deleteSignal(signal),
                                  tooltip: 'Delete call',
                                  visualDensity: VisualDensity.compact,
                                  icon: const Icon(
                                    Icons.delete_outline_rounded,
                                    size: 19,
                                    color: AtgColors.redText,
                                  ),
                                ),
                              ],
                            ),
                            if (signal.subtitle.isNotEmpty)
                              Text(
                                signal.subtitle,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: AtgText.small,
                              ),
                            const SizedBox(height: 6),
                            Row(
                              children: [
                                Expanded(
                                  child: Text(
                                    'Entry ${signal.entry.isEmpty ? '—' : signal.entry}',
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: AtgText.tiny,
                                  ),
                                ),
                                Text(
                                  signal.roomLabel,
                                  style: AtgText.tiny.copyWith(
                                    color: AtgColors.gold,
                                  ),
                                ),
                                const SizedBox(width: 8),
                                Text(
                                  timeAgo(signal.createdAt),
                                  style: AtgText.tiny,
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
        ),
      ],
    );
  }

  Widget _coursesView() {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 10),
          child: Row(
            children: [
              Text(
                _courses.isEmpty
                    ? 'No courses yet'
                    : '${_courses.length} courses',
                style: AtgText.small,
              ),
              const Spacer(),
              SizedBox(
                height: 36,
                child: FilledButton(
                  onPressed: _addCourse,
                  style: FilledButton.styleFrom(
                    backgroundColor: AtgColors.primary,
                    foregroundColor: AtgColors.ink,
                    elevation: 0,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(10),
                    ),
                    textStyle: const TextStyle(
                      fontSize: 12.5,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  child: const Text('New course'),
                ),
              ),
            ],
          ),
        ),
        Expanded(
          child: _courses.isEmpty
              ? ListView(
                  padding: const EdgeInsets.only(top: 20),
                  children: const [
                    EmptyState(
                      title: 'No courses',
                      message: 'Create the first course for the Academy.',
                      icon: Icons.school_outlined,
                    ),
                  ],
                )
              : ListView.builder(
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 28),
                  itemCount: _courses.length,
                  itemBuilder: (context, index) {
                    final course = _courses[index];
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: SurfaceCard(
                        padding: const EdgeInsets.all(13),
                        child: Row(
                          children: [
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    course.title,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: AtgText.h3,
                                  ),
                                  const SizedBox(height: 3),
                                  Text(
                                    '${course.level} · ${course.lessonsCount} lessons · ${money(course.price)}',
                                    style: AtgText.small.copyWith(
                                      fontSize: 11.5,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            IconButton(
                              onPressed: () => _deleteCourse(course),
                              tooltip: 'Delete course',
                              visualDensity: VisualDensity.compact,
                              icon: const Icon(
                                Icons.delete_outline_rounded,
                                size: 19,
                                color: AtgColors.redText,
                              ),
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
        ),
      ],
    );
  }

  Widget _settingsView() {
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 28),
      children: [
        const SectionLabel('MONEY RULES'),
        const SizedBox(height: 10),
        Row(
          children: [
            Expanded(
              child: AtgField(
                controller: _adminFee,
                label: 'Deposit fee %',
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: AtgField(
                controller: _withdrawFee,
                label: 'Withdraw fee %',
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: AtgField(
                controller: _minDeposit,
                label: 'Min deposit',
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: AtgField(
                controller: _maxDeposit,
                label: 'Max deposit',
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        AtgField(
          controller: _minWithdraw,
          label: 'Minimum withdrawal',
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
        ),
        const SizedBox(height: 22),
        const SectionLabel('HOME CONTENT'),
        const SizedBox(height: 10),
        AtgField(
          controller: _fact,
          label: "Today's fact",
          maxLines: 3,
          minLines: 2,
        ),
        const SizedBox(height: 12),
        AtgField(controller: _promoTitle, label: 'Promotion title'),
        const SizedBox(height: 12),
        AtgField(
          controller: _promoBody,
          label: 'Promotion body',
          maxLines: 3,
          minLines: 2,
        ),
        const SizedBox(height: 18),
        PrimaryButton(
          label: 'Save settings',
          loading: _savingSettings,
          onPressed: _saveSettings,
        ),
      ],
    );
  }

  Widget _accountView() {
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 28),
      children: [
        const SectionLabel('SIGNED IN AS'),
        const SizedBox(height: 10),
        SurfaceCard(
          padding: const EdgeInsets.all(13),
          child: Row(
            children: [
              AvatarCircle(text: initials(session.name), size: 40),
              const SizedBox(width: 11),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      session.name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: AtgText.h3,
                    ),
                    const SizedBox(height: 2),
                    Text(
                      session.email,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: AtgText.small.copyWith(fontSize: 11.5),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              const AtgPill(
                text: 'ADMIN',
                background: AtgColors.primarySoft,
                foreground: AtgColors.gold,
                fontSize: 9,
              ),
            ],
          ),
        ),
        const SizedBox(height: 22),
        const SectionLabel('CHANGE LOGIN CREDENTIALS'),
        const SizedBox(height: 10),
        AtgField(
          controller: _currentPassword,
          label: 'Current password',
          obscureText: true,
          autofillHints: const [AutofillHints.password],
        ),
        const SizedBox(height: 12),
        AtgField(
          controller: _newEmail,
          label: 'New email (blank keeps current)',
          keyboardType: TextInputType.emailAddress,
        ),
        const SizedBox(height: 12),
        AtgField(
          controller: _newPassword,
          label: 'New password (blank keeps current)',
          obscureText: true,
          autofillHints: const [AutofillHints.newPassword],
        ),
        const SizedBox(height: 12),
        AtgField(
          controller: _confirmPassword,
          label: 'Confirm new password',
          obscureText: true,
        ),
        const SizedBox(height: 18),
        PrimaryButton(
          label: 'Save credentials',
          loading: _savingAccount,
          onPressed: _saveAccount,
        ),
        const SizedBox(height: 10),
        Text(
          'You stay signed in. Use the new email and password next time you log in.',
          style: AtgText.small,
        ),
      ],
    );
  }
}

class _AdminRequest {
  const _AdminRequest({
    required this.kind,
    required this.id,
    required this.title,
    required this.meta,
    required this.amount,
    required this.status,
  });

  final String kind;
  final int id;
  final String title;
  final String meta;
  final double amount;
  final String status;
}

class _SignalFormSheet extends StatefulWidget {
  const _SignalFormSheet();

  @override
  State<_SignalFormSheet> createState() => _SignalFormSheetState();
}

class _SignalFormSheetState extends State<_SignalFormSheet> {
  final _title = TextEditingController();
  final _subtitle = TextEditingController();
  final _symbol = TextEditingController(text: 'EURUSD');
  final _room = TextEditingController(text: 'OPEN FLOOR');
  final _entry = TextEditingController(text: 'At market, now');
  final _stop = TextEditingController(text: 'Not given');
  final _targets = TextEditingController(text: 'Not given');
  final _rule = TextEditingController(text: 'M10 rule');
  final _participants = TextEditingController(text: '1');

  String _side = 'BUY';
  String _status = 'LIVE';

  @override
  void dispose() {
    _title.dispose();
    _subtitle.dispose();
    _symbol.dispose();
    _room.dispose();
    _entry.dispose();
    _stop.dispose();
    _targets.dispose();
    _rule.dispose();
    _participants.dispose();
    super.dispose();
  }

  void _publish() {
    if (_title.text.trim().isEmpty) {
      showToast(context, 'Give the signal a title');
      return;
    }
    Navigator.of(context).pop(
      Signal(
        id: 0,
        title: _title.text.trim(),
        subtitle: _subtitle.text.trim(),
        symbol: _symbol.text.trim().toUpperCase(),
        side: _side,
        status: _status,
        roomLabel: _room.text.trim().isEmpty ? 'OPEN FLOOR' : _room.text.trim(),
        entry: _entry.text.trim(),
        stopLoss: _stop.text.trim(),
        targets: _targets.text.trim(),
        ruleName: _rule.text.trim(),
        participants: parseAmount(_participants.text, 1).round(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
      ),
      child: ListView(
        shrinkWrap: true,
        padding: const EdgeInsets.fromLTRB(16, 14, 16, 24),
        children: [
          Center(
            child: Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: AtgColors.stroke,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          const SizedBox(height: 14),
          const Text('New signal', style: AtgText.pageTitle),
          const SizedBox(height: 14),
          AtgField(
            controller: _title,
            label: 'Title',
            textCapitalization: TextCapitalization.sentences,
          ),
          const SizedBox(height: 10),
          AtgField(
            controller: _subtitle,
            label: 'Subtitle',
            textCapitalization: TextCapitalization.sentences,
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: AtgField(
                  controller: _symbol,
                  label: 'Symbol',
                  textCapitalization: TextCapitalization.characters,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: AtgField(controller: _room, label: 'Room label'),
              ),
            ],
          ),
          const SizedBox(height: 14),
          const SectionLabel('SIDE'),
          const SizedBox(height: 8),
          AtgChipRow(
            labels: const ['BUY', 'SELL'],
            selected: _side == 'BUY' ? 0 : 1,
            onSelected: (index) =>
                setState(() => _side = index == 0 ? 'BUY' : 'SELL'),
          ),
          const SizedBox(height: 14),
          const SectionLabel('STATUS'),
          const SizedBox(height: 8),
          AtgChipRow(
            labels: const ['Live', 'Open', 'Inner', 'Closed'],
            selected: const [
              'LIVE',
              'OPEN',
              'INNER',
              'CLOSED',
            ].indexOf(_status),
            onSelected: (index) => setState(
              () => _status = const ['LIVE', 'OPEN', 'INNER', 'CLOSED'][index],
            ),
          ),
          const SizedBox(height: 14),
          AtgField(controller: _entry, label: 'Entry'),
          const SizedBox(height: 10),
          AtgField(controller: _stop, label: 'Stop loss'),
          const SizedBox(height: 10),
          AtgField(controller: _targets, label: 'Targets'),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: AtgField(controller: _rule, label: 'Rule name'),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: AtgField(
                  controller: _participants,
                  label: 'Participants',
                  keyboardType: TextInputType.number,
                ),
              ),
            ],
          ),
          const SizedBox(height: 18),
          PrimaryButton(label: 'Publish', onPressed: _publish),
        ],
      ),
    );
  }
}

class _CourseDraft {
  const _CourseDraft({
    required this.title,
    required this.subtitle,
    required this.description,
    required this.price,
    required this.level,
    required this.imageUrl,
    required this.lessons,
  });

  final String title;
  final String subtitle;
  final String description;
  final double price;
  final String level;
  final String imageUrl;
  final List<Map<String, Object?>> lessons;
}

class _CourseFormSheet extends StatefulWidget {
  const _CourseFormSheet();

  @override
  State<_CourseFormSheet> createState() => _CourseFormSheetState();
}

class _CourseFormSheetState extends State<_CourseFormSheet> {
  final _title = TextEditingController();
  final _subtitle = TextEditingController();
  final _description = TextEditingController();
  final _price = TextEditingController(text: '30');
  final _lessons = TextEditingController(
    text:
        'Welcome and chart basics | 12 | Why price moves, how to read a candle and set up your charts.\n'
        'The M10 rule | 18 | Entries, stops and targets on the 10-minute chart.\n'
        'Risk and review | 15 | Position sizing, journaling and weekly review.',
  );

  String _level = 'Beginner';
  String _imageUrl = '';
  bool _uploadingCover = false;

  @override
  void dispose() {
    _title.dispose();
    _subtitle.dispose();
    _description.dispose();
    _price.dispose();
    _lessons.dispose();
    super.dispose();
  }

  List<Map<String, Object?>> _parseLessons() {
    final items = <Map<String, Object?>>[];
    for (final line in _lessons.text.split(RegExp(r'\r?\n'))) {
      final trimmed = line.trim();
      if (trimmed.isEmpty) continue;
      final parts = trimmed.split('|');
      final title = parts[0].trim();
      if (title.isEmpty) continue;
      final duration = parts.length > 1
          ? parseAmount(parts[1], 10).round()
          : 10;
      final content = parts.length > 2 ? parts[2].trim() : '';
      final video = parts.length > 3 ? parts[3].trim() : '';
      items.add({
        'title': title,
        'durationMin': duration,
        'content': content,
        if (video.isNotEmpty) 'videoUrl': video,
      });
    }
    return items;
  }

  Future<void> _pickCover() async {
    if (_uploadingCover) return;
    XFile? file;
    try {
      file = await ImagePicker().pickImage(
        source: ImageSource.gallery,
        imageQuality: 85,
        maxWidth: 1600,
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
    if (file == null || !mounted) return;

    setState(() => _uploadingCover = true);
    try {
      final url = await api.uploadImage(file);
      if (!mounted) return;
      setState(() {
        _imageUrl = url;
        _uploadingCover = false;
      });
      showToast(context, 'Cover uploaded');
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _uploadingCover = false);
      showMessage(context, 'Could not upload the cover', e.message);
    }
  }

  void _create() {
    if (_title.text.trim().isEmpty) {
      showToast(context, 'Give the course a title');
      return;
    }
    Navigator.of(context).pop(
      _CourseDraft(
        title: _title.text.trim(),
        subtitle: _subtitle.text.trim(),
        description: _description.text.trim(),
        price: parseAmount(_price.text),
        level: _level,
        imageUrl: _imageUrl,
        lessons: _parseLessons(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
      ),
      child: ListView(
        shrinkWrap: true,
        padding: const EdgeInsets.fromLTRB(16, 14, 16, 24),
        children: [
          Center(
            child: Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: AtgColors.stroke,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          const SizedBox(height: 14),
          const Text('New course', style: AtgText.pageTitle),
          const SizedBox(height: 14),
          AtgField(
            controller: _title,
            label: 'Title',
            textCapitalization: TextCapitalization.sentences,
          ),
          const SizedBox(height: 10),
          AtgField(
            controller: _subtitle,
            label: 'Subtitle',
            textCapitalization: TextCapitalization.sentences,
          ),
          const SizedBox(height: 10),
          AtgField(
            controller: _description,
            label: 'Description',
            maxLines: 3,
            minLines: 2,
          ),
          const SizedBox(height: 10),
          AtgField(
            controller: _price,
            label: 'Price (USD)',
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
          ),
          const SizedBox(height: 12),
          const SectionLabel('LEVEL'),
          const SizedBox(height: 8),
          AtgChipRow(
            labels: const ['Beginner', 'Intermediate', 'Advanced'],
            selected: const [
              'Beginner',
              'Intermediate',
              'Advanced',
            ].indexOf(_level),
            onSelected: (index) => setState(
              () => _level = const [
                'Beginner',
                'Intermediate',
                'Advanced',
              ][index],
            ),
          ),
          const SizedBox(height: 14),
          const SectionLabel('COVER IMAGE'),
          const SizedBox(height: 8),
          if (_imageUrl.isEmpty)
            OutlineButton(
              label: _uploadingCover
                  ? 'Uploading cover…'
                  : 'Choose a cover image',
              icon: Icons.image_outlined,
              height: 46,
              onPressed: _uploadingCover ? null : _pickCover,
            )
          else ...[
            CourseCover(url: _imageUrl),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: OutlineButton(
                    label: 'Change cover',
                    height: 42,
                    onPressed: _uploadingCover ? null : _pickCover,
                  ),
                ),
                const SizedBox(width: 8),
                OutlineButton(
                  label: 'Remove',
                  height: 42,
                  onPressed: _uploadingCover
                      ? null
                      : () => setState(() => _imageUrl = ''),
                ),
              ],
            ),
          ],
          const SizedBox(height: 6),
          Text(
            'JPG or PNG. It shows on the Academy card and the course page.',
            style: AtgText.small.copyWith(fontSize: 11.5),
          ),
          const SizedBox(height: 14),
          AtgField(
            controller: _lessons,
            label: 'Lessons (title | minutes | content | video URL)',
            maxLines: 8,
            minLines: 5,
            alignLabelWithHint: true,
          ),
          const SizedBox(height: 18),
          PrimaryButton(label: 'Create course', onPressed: _create),
        ],
      ),
    );
  }
}
