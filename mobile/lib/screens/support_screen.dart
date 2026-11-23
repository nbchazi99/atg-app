import 'package:flutter/material.dart';

import '../api.dart';
import '../app_state.dart';
import '../models.dart';
import '../responsive.dart';
import '../session.dart';
import '../theme.dart';
import '../widgets.dart';

class SupportScreen extends StatefulWidget {
  const SupportScreen({super.key});

  @override
  State<SupportScreen> createState() => _SupportScreenState();
}

class _SupportScreenState extends State<SupportScreen> {
  final _message = TextEditingController();
  List<SupportConversation> _conversations = const [];
  List<SupportTicket> _tickets = const [];
  List<SupportMessage> _messages = const [];
  SupportConversation? _activeConversation;
  bool _loading = true;
  bool _sending = false;
  int _tab = 0;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _message.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final results = await Future.wait([
        api.supportConversations(),
        api.supportTickets(),
      ]);
      if (!mounted) return;
      final conversations = results[0] as List<SupportConversation>;
      setState(() {
        _conversations = conversations;
        _tickets = results[1] as List<SupportTicket>;
        _loading = false;
      });
      if (_activeConversation != null) {
        await _openConversation(_activeConversation!);
      }
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() => _loading = false);
      showToast(context, error.message);
    }
  }

  Future<void> _newConversation() async {
    try {
      final conversation = await api.createSupportConversation();
      if (!mounted) return;
      setState(() {
        _activeConversation = conversation;
        _messages = const [];
        _tab = 0;
        _conversations = [conversation, ..._conversations];
      });
    } on ApiException catch (error) {
      if (mounted) showToast(context, error.message);
    }
  }

  Future<void> _openConversation(SupportConversation conversation) async {
    setState(() {
      _activeConversation = conversation;
      _loading = true;
    });
    try {
      final detail = await api.supportConversation(conversation.id);
      if (!mounted) return;
      setState(() {
        _activeConversation = detail.conversation;
        _messages = detail.messages;
        _loading = false;
      });
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() => _loading = false);
      showToast(context, error.message);
    }
  }

  Future<void> _send() async {
    final conversation = _activeConversation;
    final text = _message.text.trim();
    if (_sending || conversation == null || text.isEmpty) return;
    setState(() => _sending = true);
    try {
      final result = await api.sendSupportMessage(conversation.id, text);
      if (!mounted) return;
      _message.clear();
      setState(() {
        _activeConversation = result.conversation;
        _messages = [..._messages, result.userMessage, result.assistantMessage];
        _sending = false;
      });
      if (result.ticket != null) {
        setState(() => _tickets = [result.ticket!, ..._tickets]);
      }
      if (result.needsHuman) {
        showToast(
          context,
          result.ticket == null
              ? 'The support team can help. File a ticket to continue.'
              : 'A support ticket was opened for the team.',
        );
      }
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() => _sending = false);
      showMessage(context, 'Could not send message', error.message);
    }
  }

  Future<void> _fileTicket() async {
    var conversation = _activeConversation;
    if (conversation == null) {
      try {
        conversation = await api.createSupportConversation();
        if (!mounted) return;
        setState(() {
          _activeConversation = conversation;
          _conversations = [conversation!, ..._conversations];
          _tab = 0;
        });
      } on ApiException catch (error) {
        if (mounted) showToast(context, error.message);
        return;
      }
    }
    final created = await showDialog<SupportTicket>(
      context: context,
      builder: (_) => _SupportTicketForm(conversation: conversation!),
    );
    if (!mounted || created == null) return;
    setState(() {
      _tickets = [created, ..._tickets.where((ticket) => ticket.id != created.id)];
    });
    showToast(context, 'Ticket ${created.ticketNumber} filed');
    await _load();
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
              DetailHeader(
                title: 'Help & Support',
                trailing: IconButton(
                  tooltip: 'New conversation',
                  onPressed: _newConversation,
                  icon: const Icon(Icons.add_comment_outlined),
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                child: Row(
                  children: [
                    Expanded(
                      child: AtgChip(
                        label: 'Chat',
                        selected: _tab == 0,
                        onTap: () => setState(() => _tab = 0),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: AtgChip(
                        label: 'My tickets (${_tickets.length})',
                        selected: _tab == 1,
                        onTap: () => setState(() => _tab = 1),
                      ),
                    ),
                  ],
                ),
              ),
              Expanded(
                child: _loading
                    ? const LoadingBlock()
                    : _tab == 0
                    ? _chatView()
                    : _ticketList(),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _chatView() {
    final conversation = _activeConversation;
    if (conversation == null) {
      if (_conversations.isEmpty) {
        return ListView(
          padding: const EdgeInsets.all(16),
          children: [
            const EmptyState(
              title: 'How can we help?',
              message:
                  'Start a conversation with ATG AI, or file a ticket for the support team.',
              icon: Icons.support_agent_rounded,
            ),
            const SizedBox(height: 14),
            FilledButton.icon(
              onPressed: _newConversation,
              icon: const Icon(Icons.chat_bubble_outline),
              label: const Text('Start a conversation'),
            ),
            const SizedBox(height: 8),
            OutlinedButton.icon(
              onPressed: _fileTicket,
              icon: const Icon(Icons.confirmation_number_outlined),
              label: const Text('File a support ticket'),
            ),
          ],
        );
      }
      return ListView(
        padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
        children: [
          FilledButton.icon(
            onPressed: _newConversation,
            icon: const Icon(Icons.add_comment_outlined),
            label: const Text('New conversation'),
          ),
          const Padding(
            padding: EdgeInsets.fromLTRB(2, 20, 2, 8),
            child: SectionLabel('RECENT CONVERSATIONS'),
          ),
          for (final item in _conversations)
            Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: SurfaceCard(
                padding: const EdgeInsets.all(14),
                onTap: () => _openConversation(item),
                child: Row(
                  children: [
                    Icon(Icons.chat_outlined, color: AtgColors.gold),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            item.topic.isEmpty ? 'New conversation' : item.topic,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: AtgText.h3,
                          ),
                          const SizedBox(height: 3),
                          Text(
                            item.status.replaceAll('_', ' '),
                            style: AtgText.small,
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
      );
    }

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
          child: Row(
            children: [
              IconButton(
                tooltip: 'All conversations',
                onPressed: () => setState(() => _activeConversation = null),
                icon: const Icon(Icons.arrow_back_rounded),
              ),
              Expanded(
                child: Text(
                  conversation.topic.isEmpty
                      ? 'ATG AI Help Desk'
                      : conversation.topic,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: AtgText.h3,
                ),
              ),
              TextButton.icon(
                onPressed: _fileTicket,
                icon: const Icon(Icons.confirmation_number_outlined, size: 18),
                label: const Text('Ticket'),
              ),
            ],
          ),
        ),
        Expanded(
          child: _messages.isEmpty
              ? const Center(
                  child: Text(
                    'Ask about ATG or any general question. Account-specific issues may need the support team.',
                    textAlign: TextAlign.center,
                    style: AtgText.bodySmall,
                  ),
                )
              : ListView.builder(
                  padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
                  itemCount: _messages.length,
                  itemBuilder: (context, index) => _messageBubble(_messages[index]),
                ),
        ),
        SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 12),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Expanded(
                  child: TextField(
                    controller: _message,
                    minLines: 1,
                    maxLines: 5,
                    textCapitalization: TextCapitalization.sentences,
                    decoration: const InputDecoration(
                      hintText: 'Type your question…',
                      border: OutlineInputBorder(),
                    ),
                    onSubmitted: (_) => _send(),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton.filled(
                  onPressed: _sending ? null : _send,
                  icon: _sending
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.send_rounded),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _messageBubble(SupportMessage message) {
    final fromUser = message.authorType == 'USER';
    return Align(
      alignment: fromUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        constraints: const BoxConstraints(maxWidth: 620),
        margin: const EdgeInsets.only(bottom: 10),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
        decoration: BoxDecoration(
          color: fromUser ? AtgColors.primarySoft : AtgColors.surface,
          border: Border.all(color: AtgColors.stroke),
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (!fromUser)
              Padding(
                padding: const EdgeInsets.only(bottom: 3),
                child: Text(message.authorName, style: AtgText.tiny),
              ),
            Text(message.text, style: AtgText.bodySmall),
          ],
        ),
      ),
    );
  }

  Widget _ticketList() {
    if (_tickets.isEmpty) {
      return ListView(
        padding: const EdgeInsets.all(16),
        children: [
          const EmptyState(
            title: 'No support tickets',
            message: 'File a ticket and the ATG support team can follow up.',
            icon: Icons.confirmation_number_outlined,
          ),
          const SizedBox(height: 14),
          OutlinedButton.icon(
            onPressed: _fileTicket,
            icon: const Icon(Icons.add),
            label: const Text('File a support ticket'),
          ),
        ],
      );
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
        children: [
          for (final ticket in _tickets)
            Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: SurfaceCard(
                padding: const EdgeInsets.all(14),
                onTap: () async {
                  await Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (_) => SupportTicketScreen(ticket: ticket),
                    ),
                  );
                  if (mounted) await _load();
                },
                child: Row(
                  children: [
                    Icon(
                      Icons.confirmation_number_outlined,
                      color: AtgColors.gold,
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(ticket.subject, style: AtgText.h3),
                          const SizedBox(height: 3),
                          Text(
                            '${ticket.ticketNumber} · ${ticket.status.replaceAll('_', ' ')}',
                            style: AtgText.small,
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
}

class SupportTicketScreen extends StatefulWidget {
  const SupportTicketScreen({super.key, required this.ticket});

  final SupportTicket ticket;

  @override
  State<SupportTicketScreen> createState() => _SupportTicketScreenState();
}

class _SupportTicketScreenState extends State<SupportTicketScreen> {
  final _reply = TextEditingController();
  SupportTicketDetail? _detail;
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _reply.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final detail = await api.supportTicket(widget.ticket.id);
      if (mounted) setState(() => _detail = detail);
    } on ApiException catch (error) {
      if (mounted) showToast(context, error.message);
    }
  }

  Future<void> _sendReply() async {
    final text = _reply.text.trim();
    if (_busy || text.isEmpty) return;
    setState(() => _busy = true);
    try {
      await api.sendSupportTicketMessage(widget.ticket.id, text);
      if (!mounted) return;
      _reply.clear();
      await _load();
      if (mounted) setState(() => _busy = false);
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() => _busy = false);
      showToast(context, error.message);
    }
  }

  Future<void> _resolve() async {
    try {
      await api.resolveSupportTicket(widget.ticket.id);
      if (!mounted) return;
      await _load();
    } on ApiException catch (error) {
      if (mounted) showToast(context, error.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    final detail = _detail;
    final ticket = detail?.ticket ?? widget.ticket;
    final closed = ticket.status == 'RESOLVED' || ticket.status == 'CLOSED';
    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      body: SafeArea(
        bottom: false,
        child: PageBody(
          maxWidth: 900,
          child: Column(
            children: [
              DetailHeader(
                title: ticket.ticketNumber,
                trailing: closed
                    ? null
                    : IconButton(
                        tooltip: 'Mark resolved',
                        onPressed: _resolve,
                        icon: const Icon(Icons.check_circle_outline),
                      ),
              ),
              Expanded(
                child: detail == null
                    ? const LoadingBlock()
                    : ListView(
                        padding: const EdgeInsets.fromLTRB(16, 4, 16, 20),
                        children: [
                          SurfaceCard(
                            padding: const EdgeInsets.all(14),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(ticket.subject, style: AtgText.h2),
                                const SizedBox(height: 6),
                                Text(
                                  '${ticket.category} · ${ticket.status.replaceAll('_', ' ')}',
                                  style: AtgText.small,
                                ),
                                const SizedBox(height: 10),
                                Text(ticket.description, style: AtgText.bodySmall),
                              ],
                            ),
                          ),
                          const SizedBox(height: 14),
                          for (final message in detail.messages)
                            _messageBubble(message),
                        ],
                      ),
              ),
              if (!closed)
                SafeArea(
                  top: false,
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(12, 8, 12, 12),
                    child: Row(
                      children: [
                        Expanded(
                          child: TextField(
                            controller: _reply,
                            minLines: 1,
                            maxLines: 4,
                            decoration: const InputDecoration(
                              hintText: 'Reply to support…',
                              border: OutlineInputBorder(),
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
                                  child: CircularProgressIndicator(strokeWidth: 2),
                                )
                              : const Icon(Icons.send_rounded),
                        ),
                      ],
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _messageBubble(SupportMessage message) {
    final fromUser = message.authorType == 'USER';
    return Align(
      alignment: fromUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        constraints: const BoxConstraints(maxWidth: 620),
        margin: const EdgeInsets.only(bottom: 9),
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: fromUser ? AtgColors.primarySoft : AtgColors.surface,
          border: Border.all(color: AtgColors.stroke),
          borderRadius: BorderRadius.circular(14),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (!fromUser)
              Padding(
                padding: const EdgeInsets.only(bottom: 4),
                child: Text(message.authorName, style: AtgText.tiny),
              ),
            Text(message.text, style: AtgText.bodySmall),
          ],
        ),
      ),
    );
  }
}

class _SupportTicketForm extends StatefulWidget {
  const _SupportTicketForm({required this.conversation});

  final SupportConversation conversation;

  @override
  State<_SupportTicketForm> createState() => _SupportTicketFormState();
}

class _SupportTicketFormState extends State<_SupportTicketForm> {
  final _form = GlobalKey<FormState>();
  late final TextEditingController _name =
      TextEditingController(text: session.name);
  late final TextEditingController _phone =
      TextEditingController(text: session.phone);
  late final TextEditingController _email =
      TextEditingController(text: session.email);
  late final TextEditingController _subject = TextEditingController(
    text: widget.conversation.topic,
  );
  late final TextEditingController _description = TextEditingController(
    text: widget.conversation.topic,
  );
  String _category = 'GENERAL';
  String _preferred = 'IN_APP';
  bool _saving = false;

  @override
  void dispose() {
    _name.dispose();
    _phone.dispose();
    _email.dispose();
    _subject.dispose();
    _description.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_form.currentState!.validate() || _saving) return;
    setState(() => _saving = true);
    try {
      final ticket = await api.createSupportTicket(
        conversationId: widget.conversation.id,
        contactName: _name.text.trim(),
        phone: _phone.text.trim(),
        email: _email.text.trim(),
        preferredContact: _preferred,
        category: _category,
        subject: _subject.text.trim(),
        description: _description.text.trim(),
      );
      if (mounted) Navigator.of(context).pop(ticket);
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() => _saving = false);
      showMessage(context, 'Could not file the ticket', error.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('File a support ticket'),
      content: SizedBox(
        width: 480,
        child: Form(
          key: _form,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                _field(_name, 'Your name'),
                _field(_phone, 'Phone number', keyboard: TextInputType.phone),
                _field(_email, 'Email (optional)', keyboard: TextInputType.emailAddress),
                _field(_subject, 'Subject'),
                DropdownButtonFormField<String>(
                  initialValue: _category,
                  decoration: const InputDecoration(labelText: 'Category'),
                  items: const [
                    'ACCOUNT',
                    'PAYMENTS',
                    'VIP',
                    'ACADEMY',
                    'SIGNALS',
                    'TECHNICAL',
                    'GENERAL',
                  ].map((value) {
                    return DropdownMenuItem(value: value, child: Text(value));
                  }).toList(),
                  onChanged: (value) => setState(() => _category = value ?? 'GENERAL'),
                ),
                DropdownButtonFormField<String>(
                  initialValue: _preferred,
                  decoration: const InputDecoration(
                    labelText: 'Preferred contact method',
                  ),
                  items: const [
                    DropdownMenuItem(value: 'IN_APP', child: Text('In app')),
                    DropdownMenuItem(value: 'PHONE', child: Text('Phone')),
                    DropdownMenuItem(value: 'WHATSAPP', child: Text('WhatsApp')),
                    DropdownMenuItem(value: 'EMAIL', child: Text('Email')),
                  ],
                  onChanged: (value) => setState(() => _preferred = value ?? 'IN_APP'),
                ),
                _field(_description, 'How can we help?', maxLines: 4),
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
          onPressed: _saving ? null : _submit,
          child: _saving
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Text('Submit'),
        ),
      ],
    );
  }

  Widget _field(
    TextEditingController controller,
    String label, {
    TextInputType? keyboard,
    int maxLines = 1,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: TextFormField(
        controller: controller,
        keyboardType: keyboard,
        maxLines: maxLines,
        validator: (value) =>
            value == null || value.trim().isEmpty ? 'Required' : null,
        decoration: InputDecoration(labelText: label),
      ),
    );
  }
}
