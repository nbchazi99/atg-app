import 'package:flutter/material.dart';

import '../api.dart';
import '../app_state.dart';
import '../models.dart';
import '../realtime.dart';
import '../theme.dart';
import '../widgets.dart';

class AdminSupportScreen extends StatefulWidget {
  const AdminSupportScreen({super.key});

  @override
  State<AdminSupportScreen> createState() => _AdminSupportScreenState();
}

class _AdminSupportScreenState extends State<AdminSupportScreen> {
  List<SupportTicket> _tickets = const [];
  List<SupportKnowledgeEntry> _knowledge = const [];
  bool _loading = true;
  int _tab = 0;
  VoidCallback? _unbind;

  @override
  void initState() {
    super.initState();
    _load();
    _unbind = realtime.bind({AtgScope.support}, _load);
  }

  @override
  void dispose() {
    _unbind?.call();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final results = await Future.wait([
        api.adminSupportTickets(),
        api.supportKnowledge(),
      ]);
      if (!mounted) return;
      setState(() {
        _tickets = results[0] as List<SupportTicket>;
        _knowledge = results[1] as List<SupportKnowledgeEntry>;
        _loading = false;
      });
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() => _loading = false);
      showToast(context, error.message);
    }
  }

  Future<void> _editKnowledge([SupportKnowledgeEntry? entry]) async {
    final saved = await showDialog<SupportKnowledgeEntry>(
      context: context,
      builder: (_) => _KnowledgeEntryDialog(entry: entry),
    );
    if (!mounted || saved == null) return;
    await _load();
  }

  Future<void> _deleteKnowledge(SupportKnowledgeEntry entry) async {
    final confirmed = await confirmAction(
      context,
      title: 'Delete knowledge entry?',
      message: '“${entry.title}” will no longer be used by the support engine.',
      confirmLabel: 'Delete',
      destructive: true,
    );
    if (!confirmed) return;
    try {
      await api.deleteSupportKnowledge(entry.id);
      if (mounted) await _load();
    } on ApiException catch (error) {
      if (mounted) showToast(context, error.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const LoadingBlock();
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 10),
          child: Row(
            children: [
              Expanded(
                child: AtgChip(
                  label: 'Ticket inbox (${_tickets.length})',
                  selected: _tab == 0,
                  onTap: () => setState(() => _tab = 0),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: AtgChip(
                  label: 'Knowledge (${_knowledge.length})',
                  selected: _tab == 1,
                  onTap: () => setState(() => _tab = 1),
                ),
              ),
            ],
          ),
        ),
        Expanded(child: _tab == 0 ? _ticketQueue() : _knowledgeBase()),
      ],
    );
  }

  Widget _ticketQueue() {
    if (_tickets.isEmpty) {
      return const EmptyState(
        title: 'No support tickets',
        message: 'Customer escalations and requests will appear here.',
        icon: Icons.support_agent_rounded,
      );
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(16, 4, 16, 28),
        children: [
          for (final ticket in _tickets)
            Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: SurfaceCard(
                padding: const EdgeInsets.all(14),
                onTap: () async {
                  await Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (_) => AdminSupportTicketScreen(ticket: ticket),
                    ),
                  );
                  if (mounted) await _load();
                },
                child: Row(
                  children: [
                    Icon(
                      ticket.status == 'RESOLVED' || ticket.status == 'CLOSED'
                          ? Icons.check_circle_outline
                          : Icons.mark_email_unread_outlined,
                      color: ticket.priority == 'URGENT'
                          ? AtgColors.redText
                          : AtgColors.gold,
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(ticket.subject, style: AtgText.h3),
                          const SizedBox(height: 3),
                          Text(
                            '${ticket.ticketNumber} · ${ticket.contactName} · ${ticket.category}',
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: AtgText.small,
                          ),
                          const SizedBox(height: 3),
                          Text(
                            '${ticket.status.replaceAll('_', ' ')} · ${ticket.priority}',
                            style: AtgText.tiny,
                          ),
                        ],
                      ),
                    ),
                    const Icon(Icons.chevron_right_rounded),
                  ],
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _knowledgeBase() {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
          child: Align(
            alignment: Alignment.centerRight,
            child: FilledButton.icon(
              onPressed: () => _editKnowledge(),
              icon: const Icon(Icons.add),
              label: const Text('Add entry'),
            ),
          ),
        ),
        Expanded(
          child: _knowledge.isEmpty
              ? const EmptyState(
                  title: 'Knowledge base is empty',
                  message: 'Add verified ATG solutions for the support engine to use.',
                  icon: Icons.menu_book_outlined,
                )
              : ListView(
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                  children: [
                    for (final entry in _knowledge)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 8),
                        child: SurfaceCard(
                          padding: const EdgeInsets.all(14),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Expanded(
                                    child: Text(entry.title, style: AtgText.h3),
                                  ),
                                  Text(
                                    entry.active ? 'ACTIVE' : 'DISABLED',
                                    style: AtgText.tiny,
                                  ),
                                ],
                              ),
                              const SizedBox(height: 4),
                              Text(entry.category, style: AtgText.tiny),
                              const SizedBox(height: 8),
                              Text(
                                entry.content,
                                maxLines: 4,
                                overflow: TextOverflow.ellipsis,
                                style: AtgText.bodySmall,
                              ),
                              const SizedBox(height: 8),
                              Wrap(
                                spacing: 8,
                                children: [
                                  TextButton.icon(
                                    onPressed: () => _editKnowledge(entry),
                                    icon: const Icon(Icons.edit_outlined, size: 18),
                                    label: const Text('Edit'),
                                  ),
                                  TextButton.icon(
                                    onPressed: () => _deleteKnowledge(entry),
                                    icon: const Icon(Icons.delete_outline, size: 18),
                                    label: const Text('Delete'),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                      ),
                  ],
                ),
        ),
      ],
    );
  }
}

class AdminSupportTicketScreen extends StatefulWidget {
  const AdminSupportTicketScreen({super.key, required this.ticket});

  final SupportTicket ticket;

  @override
  State<AdminSupportTicketScreen> createState() =>
      _AdminSupportTicketScreenState();
}

class _AdminSupportTicketScreenState extends State<AdminSupportTicketScreen> {
  final _reply = TextEditingController();
  SupportTicketDetail? _detail;
  List<AppUser> _admins = const [];
  String _status = 'OPEN';
  String _priority = 'NORMAL';
  int? _assignedAdminId;
  bool _internalNote = false;
  bool _busy = false;

  static const _statuses = [
    'OPEN',
    'ASSIGNED',
    'IN_PROGRESS',
    'WAITING_FOR_USER',
    'RESOLVED',
    'CLOSED',
  ];
  static const _priorities = ['LOW', 'NORMAL', 'HIGH', 'URGENT'];

  @override
  void initState() {
    super.initState();
    _status = widget.ticket.status;
    _priority = widget.ticket.priority;
    _assignedAdminId = widget.ticket.assignedAdminId;
    _load();
  }

  @override
  void dispose() {
    _reply.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final results = await Future.wait([
        api.adminSupportTicket(widget.ticket.id),
        api.adminUsers(),
      ]);
      if (!mounted) return;
      final detail = results[0] as SupportTicketDetail;
      setState(() {
        _detail = detail;
        _admins = (results[1] as List<AppUser>)
            .where((user) => user.isAdmin)
            .toList();
        _status = detail.ticket.status;
        _priority = detail.ticket.priority;
        _assignedAdminId = detail.ticket.assignedAdminId;
      });
    } on ApiException catch (error) {
      if (mounted) showToast(context, error.message);
    }
  }

  Future<void> _saveTicket() async {
    setState(() => _busy = true);
    try {
      await api.updateAdminSupportTicket(
        widget.ticket.id,
        status: _status,
        assignedAdminId: _assignedAdminId,
        clearAssignedAdmin: _assignedAdminId == null,
        priority: _priority,
      );
      if (!mounted) return;
      setState(() => _busy = false);
      await _load();
      if (mounted) showToast(context, 'Ticket updated');
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() => _busy = false);
      showToast(context, error.message);
    }
  }

  Future<void> _sendReply() async {
    final text = _reply.text.trim();
    if (_busy || text.isEmpty) return;
    setState(() => _busy = true);
    try {
      await api.adminSupportReply(
        widget.ticket.id,
        text,
        internalNote: _internalNote,
      );
      if (!mounted) return;
      _reply.clear();
      setState(() => _busy = false);
      await _load();
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() => _busy = false);
      showToast(context, error.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    final ticket = _detail?.ticket ?? widget.ticket;
    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      body: SafeArea(
        bottom: false,
        child: Column(
          children: [
            DetailHeader(title: ticket.ticketNumber),
            Expanded(
              child: _detail == null
                  ? const LoadingBlock()
                  : ListView(
                      padding: const EdgeInsets.fromLTRB(16, 4, 16, 20),
                      children: [
                        _contactCard(ticket),
                        const SizedBox(height: 10),
                        SurfaceCard(
                          padding: const EdgeInsets.all(14),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(ticket.subject, style: AtgText.h2),
                              const SizedBox(height: 6),
                              Text(ticket.description, style: AtgText.bodySmall),
                            ],
                          ),
                        ),
                        const SizedBox(height: 12),
                        _ticketControls(ticket),
                        const Padding(
                          padding: EdgeInsets.fromLTRB(2, 18, 2, 8),
                          child: SectionLabel('SHARED CONVERSATION'),
                        ),
                        for (final message in _detail!.messages)
                          _messageBubble(message),
                      ],
                    ),
            ),
            if (ticket.status != 'RESOLVED' && ticket.status != 'CLOSED')
              SafeArea(
                top: false,
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(12, 8, 12, 12),
                  child: Column(
                    children: [
                      Row(
                        children: [
                          Checkbox(
                            value: _internalNote,
                            onChanged: (value) => setState(
                              () => _internalNote = value ?? false,
                            ),
                          ),
                          const Text('Internal note (hidden from customer)'),
                        ],
                      ),
                      Row(
                        children: [
                          Expanded(
                            child: TextField(
                              controller: _reply,
                              minLines: 1,
                              maxLines: 4,
                              decoration: InputDecoration(
                                hintText: _internalNote
                                    ? 'Add an internal note…'
                                    : 'Reply to customer…',
                                border: const OutlineInputBorder(),
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          IconButton.filled(
                            onPressed: _busy ? null : _sendReply,
                            icon: _busy
                                ? const SizedBox(
                                    width: 18,
                                    height: 18,
                                    child: CircularProgressIndicator(
                                      strokeWidth: 2,
                                    ),
                                  )
                                : const Icon(Icons.send_rounded),
                          ),
                        ],
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

  Widget _contactCard(SupportTicket ticket) {
    return SurfaceCard(
      padding: const EdgeInsets.all(14),
      child: Wrap(
        spacing: 16,
        runSpacing: 8,
        children: [
          Text(ticket.contactName, style: AtgText.h3),
          Text(ticket.phone, style: AtgText.small),
          if ((ticket.email ?? '').isNotEmpty)
            Text(ticket.email!, style: AtgText.small),
          Text(ticket.category, style: AtgText.small),
        ],
      ),
    );
  }

  Widget _ticketControls(SupportTicket ticket) {
    return SurfaceCard(
      padding: const EdgeInsets.all(12),
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: DropdownButtonFormField<String>(
                  initialValue: _statuses.contains(_status) ? _status : 'OPEN',
                  decoration: const InputDecoration(labelText: 'Status'),
                  items: _statuses
                      .map(
                        (value) => DropdownMenuItem(
                          value: value,
                          child: Text(value.replaceAll('_', ' ')),
                        ),
                      )
                      .toList(),
                  onChanged: (value) => setState(() => _status = value ?? _status),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: DropdownButtonFormField<String>(
                  initialValue: _priorities.contains(_priority)
                      ? _priority
                      : 'NORMAL',
                  decoration: const InputDecoration(labelText: 'Priority'),
                  items: _priorities
                      .map(
                        (value) => DropdownMenuItem(
                          value: value,
                          child: Text(value),
                        ),
                      )
                      .toList(),
                  onChanged: (value) =>
                      setState(() => _priority = value ?? _priority),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          DropdownButtonFormField<int?>(
            initialValue: _assignedAdminId,
            decoration: const InputDecoration(labelText: 'Assign administrator'),
            items: [
              const DropdownMenuItem<int?>(
                value: null,
                child: Text('Unassigned'),
              ),
              ..._admins.map(
                (admin) => DropdownMenuItem<int?>(
                  value: admin.id,
                  child: Text(admin.name),
                ),
              ),
            ],
            onChanged: (value) => setState(() => _assignedAdminId = value),
          ),
          const SizedBox(height: 8),
          Align(
            alignment: Alignment.centerRight,
            child: FilledButton(
              onPressed: _busy ? null : _saveTicket,
              child: const Text('Save ticket'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _messageBubble(SupportMessage message) {
    final internal = message.internalNote;
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: internal ? AtgColors.primarySoft : AtgColors.surface,
        border: Border.all(color: AtgColors.stroke),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '${message.authorName}${internal ? ' · INTERNAL NOTE' : ''}',
            style: AtgText.tiny,
          ),
          const SizedBox(height: 4),
          Text(message.text, style: AtgText.bodySmall),
        ],
      ),
    );
  }
}

class _KnowledgeEntryDialog extends StatefulWidget {
  const _KnowledgeEntryDialog({this.entry});

  final SupportKnowledgeEntry? entry;

  @override
  State<_KnowledgeEntryDialog> createState() => _KnowledgeEntryDialogState();
}

class _KnowledgeEntryDialogState extends State<_KnowledgeEntryDialog> {
  final _form = GlobalKey<FormState>();
  late final _title = TextEditingController(text: widget.entry?.title ?? '');
  late final _content = TextEditingController(text: widget.entry?.content ?? '');
  late final _keywords = TextEditingController(
    text: widget.entry?.keywords ?? '',
  );
  late String _category = widget.entry?.category ?? 'GENERAL';
  late bool _active = widget.entry?.active ?? true;
  bool _saving = false;

  static const _categories = [
    'ACCOUNT',
    'PAYMENTS',
    'VIP',
    'ACADEMY',
    'SIGNALS',
    'TECHNICAL',
    'GENERAL',
  ];

  @override
  void dispose() {
    _title.dispose();
    _content.dispose();
    _keywords.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_form.currentState!.validate() || _saving) return;
    setState(() => _saving = true);
    try {
      final entry = await api.saveSupportKnowledge(
        id: widget.entry?.id,
        title: _title.text.trim(),
        category: _category,
        content: _content.text.trim(),
        keywords: _keywords.text.trim(),
        active: _active,
      );
      if (mounted) Navigator.of(context).pop(entry);
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() => _saving = false);
      showMessage(context, 'Could not save knowledge entry', error.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.entry == null ? 'Add knowledge entry' : 'Edit entry'),
      content: SizedBox(
        width: 520,
        child: Form(
          key: _form,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                _field(_title, 'Title'),
                DropdownButtonFormField<String>(
                  initialValue: _categories.contains(_category)
                      ? _category
                      : 'GENERAL',
                  decoration: const InputDecoration(labelText: 'Category'),
                  items: _categories
                      .map(
                        (value) => DropdownMenuItem(
                          value: value,
                          child: Text(value),
                        ),
                      )
                      .toList(),
                  onChanged: (value) =>
                      setState(() => _category = value ?? 'GENERAL'),
                ),
                _field(_content, 'Approved ATG answer', maxLines: 7),
                _field(_keywords, 'Search keywords (comma separated)'),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  value: _active,
                  title: const Text('Available to the support engine'),
                  onChanged: (value) => setState(() => _active = value),
                ),
              ],
            ),
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: _saving ? null : () => Navigator.of(context).pop(),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: _saving ? null : _save,
          child: _saving
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Text('Save'),
        ),
      ],
    );
  }

  Widget _field(
    TextEditingController controller,
    String label, {
    int maxLines = 1,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: TextFormField(
        controller: controller,
        maxLines: maxLines,
        validator: (value) =>
            value == null || value.trim().isEmpty ? 'Required' : null,
        decoration: InputDecoration(labelText: label),
      ),
    );
  }
}
