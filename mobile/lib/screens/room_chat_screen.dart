import 'dart:async';

import 'package:flutter/material.dart';

import '../api.dart';
import '../app_state.dart';
import '../format.dart';
import '../linked_text.dart';
import '../models.dart';
import '../realtime.dart';
import '../responsive.dart';
import '../theme.dart';
import '../widgets.dart';

class RoomChatScreen extends StatefulWidget {
  const RoomChatScreen({
    super.key,
    required this.roomId,
    required this.roomName,
  });

  final int roomId;
  final String roomName;

  @override
  State<RoomChatScreen> createState() => _RoomChatScreenState();
}

class _RoomChatScreenState extends State<RoomChatScreen> {
  final _input = TextEditingController();
  final _scroll = ScrollController();
  final _focus = FocusNode();

  List<RoomMessage> _messages = const [];
  bool _loading = true;
  bool _sending = false;
  bool _fetching = false;
  bool _reloadPending = false;
  String? _error;
  bool _forbidden = false;
  VoidCallback? _unbind;

  @override
  void initState() {
    super.initState();
    _load();
    _unbind = realtime.bind({
      AtgScope.room(widget.roomId),
      AtgScope.rooms,
    }, () => _load(silent: true));
  }

  @override
  void dispose() {
    _unbind?.call();
    _input.dispose();
    _scroll.dispose();
    _focus.dispose();
    super.dispose();
  }

  Future<void> _load({bool silent = false}) async {
    if (_fetching) {
      _reloadPending = true;
      return;
    }
    _fetching = true;
    try {
      final messages = await api.messages(widget.roomId);
      if (!mounted) return;
      final changed =
          messages.length != _messages.length ||
          (messages.isNotEmpty &&
              _messages.isNotEmpty &&
              messages.last.id != _messages.last.id);
      setState(() {
        _messages = messages;
        _error = null;
        _loading = false;
      });
      if (!silent || changed) _scrollToEnd();
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.status == 403) {
        _unbind?.call();
      }
      if (!silent || _messages.isEmpty || e.status == 403) {
        setState(() {
          _error = e.message;
          _forbidden = e.status == 403;
          _loading = false;
        });
      }
    } catch (error, stackTrace) {
      debugPrint('Could not load room messages: $error\n$stackTrace');
      if (!mounted || (silent && _messages.isNotEmpty)) return;
      setState(() {
        _error =
            'Could not load messages. Check your connection and try again.';
        _forbidden = false;
        _loading = false;
      });
    } finally {
      _fetching = false;
      if (_reloadPending && mounted) {
        _reloadPending = false;
        unawaited(_load(silent: true));
      }
    }
  }

  Future<void> _retryLoad() async {
    setState(() {
      _error = null;
      _forbidden = false;
      _loading = true;
    });
    await _load();
  }

  void _scrollToEnd() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted || !_scroll.hasClients) return;
      _scroll.animateTo(
        _scroll.position.maxScrollExtent,
        duration: const Duration(milliseconds: 220),
        curve: Curves.easeOut,
      );
    });
  }

  Future<void> _send() async {
    final text = _input.text.trim();
    if (text.isEmpty || _sending) return;
    setState(() => _sending = true);
    try {
      final message = await api.sendMessage(widget.roomId, text);
      if (!mounted) return;
      _input.clear();
      setState(() {
        _messages = [..._messages, message];
      });
      _scrollToEnd();
      _focus.requestFocus();
    } on ApiException catch (e) {
      if (!mounted) return;
      showToast(context, e.message);
    } catch (error, stackTrace) {
      debugPrint('Could not send room message: $error\n$stackTrace');
      if (mounted) showToast(context, 'Could not send message. Try again.');
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final me = session.name;

    return Scaffold(
      backgroundColor: AtgColors.bg,
      body: SafeArea(
        child: PageBody(
          maxWidth: 900,
          child: Column(
            children: [
              DetailHeader(
                title: widget.roomName,
                trailing: const LiveDot(showLabel: true),
              ),
              Expanded(
                child: _loading
                    ? const LoadingBlock()
                    : _error != null
                    ? ListView(
                        padding: const EdgeInsets.all(24),
                        children: [
                          EmptyState(
                            title: _forbidden
                                ? 'Members only'
                                : 'Could not load chat',
                            message: _error!,
                            icon: _forbidden
                                ? Icons.lock_outline_rounded
                                : Icons.cloud_off_outlined,
                          ),
                          if (!_forbidden)
                            OutlineButton(
                              label: 'Try again',
                              onPressed: _retryLoad,
                            ),
                        ],
                      )
                    : _messages.isEmpty
                    ? ListView(
                        physics: const AlwaysScrollableScrollPhysics(),
                        children: const [
                          EmptyState(
                            title: 'The room is quiet',
                            message: 'Say hello. The desk and other traders see your message here.',
                            icon: Icons.forum_outlined,
                          ),
                        ],
                      )
                    : ListView.builder(
                        controller: _scroll,
                        padding: const EdgeInsets.fromLTRB(16, 10, 16, 12),
                        itemCount: _messages.length,
                        itemBuilder: (context, index) {
                          final message = _messages[index];
                          final mine = message.author == me;
                          return _Bubble(message: message, mine: mine);
                        },
                      ),
              ),
              if (_error == null) _buildComposer(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildComposer() {
    return Container(
      padding: const EdgeInsets.fromLTRB(12, 8, 12, 10),
      decoration: const BoxDecoration(
        color: AtgColors.surface,
        border: Border(top: BorderSide(color: AtgColors.stroke)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Expanded(
            child: TextField(
              controller: _input,
              focusNode: _focus,
              minLines: 1,
              maxLines: 4,
              textCapitalization: TextCapitalization.sentences,
              textInputAction: TextInputAction.send,
              onSubmitted: (_) => _send(),
              style: const TextStyle(fontSize: 14, color: AtgColors.ink),
              decoration: atgInputDecoration(hint: 'Message the room').copyWith(
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 14,
                  vertical: 12,
                ),
              ),
            ),
          ),
          const SizedBox(width: 8),
          SizedBox(
            width: 46,
            height: 46,
            child: FilledButton(
              onPressed: _sending ? null : _send,
              style: FilledButton.styleFrom(
                backgroundColor: AtgColors.primary,
                foregroundColor: AtgColors.ink,
                disabledBackgroundColor: AtgColors.primary.withValues(
                  alpha: 0.45,
                ),
                elevation: 0,
                padding: EdgeInsets.zero,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(13),
                ),
              ),
              child: _sending
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: AtgColors.ink,
                      ),
                    )
                  : const Icon(Icons.send_rounded, size: 19),
            ),
          ),
        ],
      ),
    );
  }
}

class _Bubble extends StatelessWidget {
  const _Bubble({required this.message, required this.mine});

  final RoomMessage message;
  final bool mine;

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: mine ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        constraints: BoxConstraints(
          maxWidth: MediaQuery.of(context).size.width * 0.82,
        ),
        margin: const EdgeInsets.only(bottom: 8),
        padding: const EdgeInsets.fromLTRB(13, 9, 13, 9),
        decoration: BoxDecoration(
          color: mine ? AtgColors.primarySoft : AtgColors.surface,
          border: Border.all(
            color: mine ? AtgColors.primary : AtgColors.stroke,
          ),
          borderRadius: BorderRadius.only(
            topLeft: const Radius.circular(15),
            topRight: const Radius.circular(15),
            bottomLeft: Radius.circular(mine ? 15 : 4),
            bottomRight: Radius.circular(mine ? 4 : 15),
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  mine ? 'You' : message.author,
                  style: const TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w800,
                    color: AtgColors.gold,
                  ),
                ),
                const SizedBox(width: 8),
                Text(
                  timeAgo(message.createdAt),
                  style: AtgText.tiny.copyWith(fontSize: 10),
                ),
              ],
            ),
            const SizedBox(height: 3),
            LinkedText(
              message.text,
              style: const TextStyle(
                fontSize: 14,
                color: AtgColors.ink,
                height: 1.4,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
