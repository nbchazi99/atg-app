import 'dart:async';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

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
    this.threadId,
    this.threadTitle,
    this.threadOpen = true,
    this.canManageThread = false,
    this.onStartThread,
  });

  final int roomId;
  final String roomName;
  final int? threadId;
  final String? threadTitle;
  final bool threadOpen;
  final bool canManageThread;
  final VoidCallback? onStartThread;

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
  bool _uploadingImage = false;
  late bool _threadOpen;
  bool _fetching = false;
  bool _reloadPending = false;
  String? _error;
  bool _forbidden = false;
  VoidCallback? _unbind;
  Timer? _fallbackPoll;

  @override
  void initState() {
    super.initState();
    _threadOpen = widget.threadOpen;
    _load();
    _unbind = realtime.bind({
      AtgScope.room(widget.roomId),
      AtgScope.rooms,
    }, () => _load(silent: true));
    _fallbackPoll = Timer.periodic(const Duration(seconds: 5), (_) {
      if (!realtime.connected.value) unawaited(_load(silent: true));
    });
  }

  @override
  void dispose() {
    _unbind?.call();
    _fallbackPoll?.cancel();
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
      final messages = await api.messages(
        widget.roomId,
        threadId: widget.threadId,
      );
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

  Future<void> _send({String? attachmentUrl}) async {
    final text = _input.text.trim();
    if ((text.isEmpty && attachmentUrl == null) || _sending || !_threadOpen) {
      return;
    }
    setState(() => _sending = true);
    try {
      final message = await api.sendMessage(
        widget.roomId,
        text,
        threadId: widget.threadId,
        attachmentUrl: attachmentUrl,
      );
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

  Future<void> _pickAndSendImage() async {
    if (_sending || _uploadingImage || !_threadOpen) return;
    setState(() => _uploadingImage = true);
    try {
      final image = await ImagePicker().pickImage(
        source: ImageSource.gallery,
        imageQuality: 85,
      );
      if (image == null || !mounted) return;
      final url = await api.uploadImage(image);
      if (mounted) await _send(attachmentUrl: url);
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    } catch (error, stackTrace) {
      debugPrint('Could not attach a chat image: $error\n$stackTrace');
      if (mounted) showToast(context, 'Could not attach the image. Try again.');
    } finally {
      if (mounted) setState(() => _uploadingImage = false);
    }
  }

  Future<void> _toggleThread() async {
    final threadId = widget.threadId;
    if (threadId == null || !widget.canManageThread) return;
    try {
      final thread = await api.setThreadOpen(
        roomId: widget.roomId,
        threadId: threadId,
        open: !_threadOpen,
      );
      if (!mounted) return;
      setState(() => _threadOpen = thread.open);
      showToast(
        context,
        thread.open ? 'Conversation reopened' : 'Conversation closed',
      );
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    } catch (error, stackTrace) {
      debugPrint('Could not update conversation status: $error\n$stackTrace');
      if (mounted) {
        showToast(context, 'Could not update conversation. Try again.');
      }
    }
  }

  Future<void> _deleteMessage(RoomMessage message) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Delete message'),
        content: const Text('This message will be removed from the room.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: const Text('Delete'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      await api.deleteRoomMessage(widget.roomId, message.id);
      await _load(silent: true);
      if (mounted) showToast(context, 'Message deleted');
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
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
                title: widget.threadTitle ?? widget.roomName,
                trailing: widget.threadId == null
                    ? const LiveDot(showLabel: true)
                    : Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8,
                              vertical: 5,
                            ),
                            decoration: BoxDecoration(
                              color: _threadOpen
                                  ? AtgColors.greenBg
                                  : AtgColors.redBg,
                              borderRadius: BorderRadius.circular(20),
                            ),
                            child: Text(
                              _threadOpen ? 'OPEN' : 'CLOSED',
                              style: TextStyle(
                                color: _threadOpen
                                    ? AtgColors.greenText
                                    : AtgColors.redText,
                                fontSize: 9,
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                          ),
                          if (widget.canManageThread)
                            TextButton(
                              onPressed: _toggleThread,
                              child: Text(_threadOpen ? 'Close' : 'Reopen'),
                            ),
                        ],
                      ),
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
                          final mine =
                              (message.authorId != null &&
                                  message.authorId == session.id) ||
                              (message.authorId == null &&
                                  message.author == me);
                          return _Bubble(
                            message: message,
                            mine: mine,
                            canDelete: session.isAdmin,
                            onDelete: () => _deleteMessage(message),
                          );
                        },
                      ),
              ),
              if (_error == null &&
                  widget.threadId == null &&
                  widget.onStartThread != null)
                Padding(
                  padding: const EdgeInsets.fromLTRB(12, 8, 12, 0),
                  child: SizedBox(
                    width: double.infinity,
                    child: OutlinedButton.icon(
                      onPressed: widget.onStartThread,
                      icon: const Icon(Icons.add_comment_outlined),
                      label: const Text('Start a new Community chat'),
                    ),
                  ),
                ),
              if (_error == null)
                _threadOpen ? _buildComposer() : _closedNotice(),
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
          IconButton(
            tooltip: 'Attach a photo',
            onPressed: _sending || _uploadingImage ? null : _pickAndSendImage,
            icon: _uploadingImage
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.image_outlined),
          ),
          Expanded(
            child: TextField(
              controller: _input,
              focusNode: _focus,
              enabled: !_sending && !_uploadingImage,
              minLines: 1,
              maxLines: 4,
              textCapitalization: TextCapitalization.sentences,
              textInputAction: TextInputAction.send,
              onSubmitted: (_) => _send(),
              style: const TextStyle(fontSize: 14, color: AtgColors.ink),
              decoration: atgInputDecoration(hint: 'Message or paste a link')
                  .copyWith(
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
              onPressed: _sending || _uploadingImage ? null : _send,
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

  Widget _closedNotice() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: const BoxDecoration(
        color: AtgColors.surface,
        border: Border(top: BorderSide(color: AtgColors.stroke)),
      ),
      child: const Text(
        'This conversation is closed. Members can still read the messages.',
        textAlign: TextAlign.center,
        style: AtgText.small,
      ),
    );
  }
}

class _Bubble extends StatelessWidget {
  const _Bubble({
    required this.message,
    required this.mine,
    required this.canDelete,
    required this.onDelete,
  });

  final RoomMessage message;
  final bool mine;
  final bool canDelete;
  final VoidCallback onDelete;

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
                CircleAvatar(
                  radius: 12,
                  backgroundColor: AtgColors.primarySoft,
                  backgroundImage: message.authorAvatarUrl?.isNotEmpty == true
                      ? NetworkImage(mediaUrl(message.authorAvatarUrl!))
                      : null,
                  child: message.authorAvatarUrl?.isNotEmpty == true
                      ? null
                      : Text(
                          (message.author.isNotEmpty
                                  ? message.author.substring(0, 1)
                                  : 'T')
                              .toUpperCase(),
                          style: const TextStyle(
                            fontSize: 9,
                            fontWeight: FontWeight.w800,
                            color: AtgColors.gold,
                          ),
                        ),
                ),
                const SizedBox(width: 7),
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
                if (mine) ...[
                  const SizedBox(width: 6),
                  const Icon(
                    Icons.done_rounded,
                    size: 12,
                    color: AtgColors.greenText,
                  ),
                  const SizedBox(width: 2),
                  Text(
                    'Sent',
                    style: AtgText.tiny.copyWith(color: AtgColors.greenText),
                  ),
                ],
                if (canDelete) ...[
                  const SizedBox(width: 4),
                  InkWell(
                    onTap: onDelete,
                    borderRadius: BorderRadius.circular(12),
                    child: const Padding(
                      padding: EdgeInsets.all(3),
                      child: Icon(
                        Icons.delete_outline_rounded,
                        size: 16,
                        color: AtgColors.muted,
                      ),
                    ),
                  ),
                ],
              ],
            ),
            const SizedBox(height: 3),
            if (message.attachmentUrl?.isNotEmpty == true) ...[
              ClipRRect(
                borderRadius: BorderRadius.circular(9),
                child: ConstrainedBox(
                  constraints: const BoxConstraints(
                    maxWidth: 260,
                    maxHeight: 260,
                  ),
                  child: Image.network(
                    mediaUrl(message.attachmentUrl!),
                    fit: BoxFit.contain,
                    loadingBuilder: (context, child, progress) {
                      if (progress == null) return child;
                      return const SizedBox(
                        width: 220,
                        height: 140,
                        child: Center(
                          child: CircularProgressIndicator(strokeWidth: 2),
                        ),
                      );
                    },
                    errorBuilder: (context, error, stackTrace) =>
                        const SizedBox(
                          width: 220,
                          height: 80,
                          child: Center(
                            child: Text('Image could not be loaded'),
                          ),
                        ),
                  ),
                ),
              ),
              if (message.text.isNotEmpty) const SizedBox(height: 7),
            ],
            if (message.text.isNotEmpty)
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
