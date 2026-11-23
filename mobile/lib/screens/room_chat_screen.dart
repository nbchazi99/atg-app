import 'dart:async';

import 'package:audioplayers/audioplayers.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:image_picker/image_picker.dart';
import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';
import 'package:url_launcher/url_launcher.dart';

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
  });

  final int roomId;
  final String roomName;
  final int? threadId;
  final String? threadTitle;
  final bool threadOpen;
  final bool canManageThread;

  @override
  State<RoomChatScreen> createState() => _RoomChatScreenState();
}

class _RoomChatScreenState extends State<RoomChatScreen> {
  final _input = TextEditingController();
  final _scroll = ScrollController();
  final _focus = FocusNode();
  final _recorder = AudioRecorder();
  final _audioPlayer = AudioPlayer();

  List<RoomMessage> _messages = const [];
  bool _loading = true;
  bool _sending = false;
  bool _uploadingAttachment = false;
  bool _recording = false;
  String? _playingAudioUrl;
  StreamSubscription<void>? _audioCompleteSubscription;
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
    _audioCompleteSubscription = _audioPlayer.onPlayerComplete.listen((_) {
      if (mounted) setState(() => _playingAudioUrl = null);
    });
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
    _audioCompleteSubscription?.cancel();
    unawaited(_audioPlayer.dispose());
    if (_recording) unawaited(_recorder.cancel());
    unawaited(_recorder.dispose());
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

  Future<void> _send({String? attachmentUrl, String? attachmentType}) async {
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
        attachmentType: attachmentType,
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

  Future<void> _pickAndSendAttachment({bool imagesOnly = false}) async {
    if (_sending || _uploadingAttachment || _recording || !_threadOpen) return;
    setState(() => _uploadingAttachment = true);
    try {
      final files = await FilePicker.pickFiles(
        type: FileType.custom,
        allowedExtensions: imagesOnly
            ? const ['jpg', 'jpeg', 'png', 'webp', 'gif']
            : const [
                'jpg',
                'jpeg',
                'png',
                'webp',
                'gif',
                'mp4',
                'webm',
                'mov',
                'm4v',
                'pdf',
                'doc',
                'docx',
                'xls',
                'xlsx',
                'ppt',
                'pptx',
                'mp3',
                'wav',
                'm4a',
                'aac',
                'ogg',
                'opus',
                'flac',
              ],
      );
      if (files.isEmpty || !mounted) return;
      final url = await api.uploadRoomAttachment(widget.roomId, files.first);
      if (mounted) await _send(attachmentUrl: url);
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    } catch (error, stackTrace) {
      debugPrint('Could not attach a chat file: $error\n$stackTrace');
      if (mounted) showToast(context, 'Could not attach the file. Try again.');
    } finally {
      if (mounted) setState(() => _uploadingAttachment = false);
    }
  }

  Future<void> _toggleVoiceRecording() async {
    if (_sending || _uploadingAttachment || !_threadOpen) return;
    if (_recording) {
      await _stopAndSendVoiceNote();
      return;
    }
    try {
      if (!await _recorder.hasPermission()) {
        if (mounted) showToast(context, 'Microphone permission is required.');
        return;
      }
      if (!mounted) return;
      final filename =
          'voice-${DateTime.now().millisecondsSinceEpoch}.${kIsWeb ? 'webm' : 'm4a'}';
      var path = '';
      if (!kIsWeb) {
        final directory = await getTemporaryDirectory();
        path = '${directory.path}/$filename';
      }
      await _recorder.start(
        RecordConfig(encoder: kIsWeb ? AudioEncoder.opus : AudioEncoder.aacLc),
        path: path,
      );
      if (mounted) setState(() => _recording = true);
    } catch (error, stackTrace) {
      debugPrint('Could not start voice recording: $error\n$stackTrace');
      if (mounted) showToast(context, 'Could not start recording.');
    }
  }

  Future<void> _stopAndSendVoiceNote() async {
    try {
      final path = await _recorder.stop();
      if (!mounted) return;
      setState(() => _recording = false);
      if (path == null || path.isEmpty) {
        if (mounted) showToast(context, 'The voice note was empty.');
        return;
      }
      final filename =
          'voice-${DateTime.now().millisecondsSinceEpoch}.${kIsWeb ? 'webm' : 'm4a'}';
      setState(() => _uploadingAttachment = true);
      final file = XFile(path, name: filename);
      final url = await api.uploadRoomRecording(widget.roomId, file);
      if (mounted) {
        await _send(attachmentUrl: url, attachmentType: 'AUDIO');
      }
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    } catch (error, stackTrace) {
      debugPrint('Could not send voice note: $error\n$stackTrace');
      if (mounted) showToast(context, 'Could not send the voice note.');
    } finally {
      if (mounted) {
        setState(() {
          _recording = false;
          _uploadingAttachment = false;
        });
      }
    }
  }

  Future<void> _toggleAudio(String attachmentUrl) async {
    try {
      if (_playingAudioUrl == attachmentUrl) {
        await _audioPlayer.stop();
        if (mounted) setState(() => _playingAudioUrl = null);
        return;
      }
      await _audioPlayer.stop();
      if (!mounted) return;
      setState(() => _playingAudioUrl = attachmentUrl);
      await _audioPlayer.play(UrlSource(mediaUrl(attachmentUrl)));
    } catch (error, stackTrace) {
      debugPrint('Could not play chat audio: $error\n$stackTrace');
      if (mounted) {
        setState(() => _playingAudioUrl = null);
        showToast(context, 'Could not play this audio file.');
      }
    }
  }

  Future<void> _openAttachment(String attachmentUrl) async {
    try {
      final opened = await launchUrl(
        Uri.parse(mediaUrl(attachmentUrl)),
        mode: LaunchMode.externalApplication,
      );
      if (!opened && mounted) {
        showToast(context, 'No app is available to open this attachment.');
      }
    } catch (error, stackTrace) {
      debugPrint('Could not open chat attachment: $error\n$stackTrace');
      if (mounted) showToast(context, 'Could not open this attachment.');
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
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
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
                            audioPlaying:
                                _playingAudioUrl == message.attachmentUrl,
                            onPlayAudio: () =>
                                _toggleAudio(message.attachmentUrl!),
                            onOpenAttachment: () =>
                                _openAttachment(message.attachmentUrl!),
                            onDelete: () => _deleteMessage(message),
                          );
                        },
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
      decoration: BoxDecoration(
        color: AtgColors.surface,
        border: Border(top: BorderSide(color: AtgColors.stroke)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          IconButton(
            tooltip: 'Attach a photo',
            onPressed: _sending || _uploadingAttachment || _recording
                ? null
                : () => _pickAndSendAttachment(imagesOnly: true),
            icon: _uploadingAttachment
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.image_outlined),
          ),
          IconButton(
            tooltip: 'Attach a file',
            onPressed: _sending || _uploadingAttachment || _recording
                ? null
                : _pickAndSendAttachment,
            icon: const Icon(Icons.attach_file_rounded),
          ),
          IconButton(
            tooltip: _recording
                ? 'Stop and send voice note'
                : 'Record voice note',
            onPressed: _uploadingAttachment || _sending
                ? null
                : _toggleVoiceRecording,
            icon: Icon(
              _recording ? Icons.stop_circle_outlined : Icons.mic_none_rounded,
              color: _recording ? AtgColors.redText : null,
            ),
          ),
          Expanded(
            child: TextField(
              controller: _input,
              focusNode: _focus,
              enabled: !_sending && !_uploadingAttachment,
              minLines: 1,
              maxLines: 4,
              textCapitalization: TextCapitalization.sentences,
              textInputAction: TextInputAction.send,
              onSubmitted: (_) => _send(),
              style: TextStyle(fontSize: 14, color: AtgColors.ink),
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
              onPressed: _sending || _uploadingAttachment || _recording
                  ? null
                  : _send,
              style: FilledButton.styleFrom(
                backgroundColor: AtgColors.primary,
                foregroundColor: AtgColors.primaryForeground,
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
                  ? SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: AtgColors.primaryForeground,
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
      decoration: BoxDecoration(
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
    required this.audioPlaying,
    required this.onPlayAudio,
    required this.onOpenAttachment,
    required this.onDelete,
  });

  final RoomMessage message;
  final bool mine;
  final bool canDelete;
  final bool audioPlaying;
  final VoidCallback onPlayAudio;
  final VoidCallback onOpenAttachment;
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
                          style: TextStyle(
                            fontSize: 9,
                            fontWeight: FontWeight.w800,
                            color: AtgColors.gold,
                          ),
                        ),
                ),
                const SizedBox(width: 7),
                Text(
                  mine ? 'You' : message.author,
                  style: TextStyle(
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
                  Icon(
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
                    child: Padding(
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
              _buildAttachment(message.attachmentUrl!),
              if (message.text.isNotEmpty) const SizedBox(height: 7),
            ],
            if (message.text.isNotEmpty)
              LinkedText(
                message.text,
                style: TextStyle(
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

  Widget _buildAttachment(String url) {
    final kind = _attachmentKind(url, message.attachmentType);
    if (kind == 'image') {
      return ClipRRect(
        borderRadius: BorderRadius.circular(9),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 260, maxHeight: 260),
          child: Image.network(
            mediaUrl(url),
            fit: BoxFit.contain,
            loadingBuilder: (context, child, progress) {
              if (progress == null) return child;
              return const SizedBox(
                width: 220,
                height: 140,
                child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
              );
            },
            errorBuilder: (context, error, stackTrace) => const SizedBox(
              width: 220,
              height: 80,
              child: Center(child: Text('Image could not be loaded')),
            ),
          ),
        ),
      );
    }

    final audio = kind == 'audio';
    return OutlinedButton.icon(
      onPressed: audio ? onPlayAudio : onOpenAttachment,
      icon: Icon(
        audio
            ? audioPlaying
                  ? Icons.stop_circle_outlined
                  : Icons.play_circle_outline_rounded
            : kind == 'video'
            ? Icons.video_file_outlined
            : Icons.description_outlined,
      ),
      label: Text(
        audio
            ? audioPlaying
                  ? 'Stop voice note'
                  : 'Play voice note'
            : '${kind == 'video' ? 'Open video' : 'Open file'} · ${_attachmentName(url)}',
        overflow: TextOverflow.ellipsis,
      ),
    );
  }
}

String _attachmentKind(String url, String? type) {
  switch (type?.toUpperCase()) {
    case 'IMAGE':
      return 'image';
    case 'VIDEO':
      return 'video';
    case 'AUDIO':
      return 'audio';
    case 'FILE':
      return 'file';
  }
  final uri = Uri.tryParse(url);
  final path = (uri?.path ?? url).toLowerCase();
  final dot = path.lastIndexOf('.');
  final extension = dot < 0 ? '' : path.substring(dot);
  if (const {'.jpg', '.jpeg', '.png', '.webp', '.gif'}.contains(extension)) {
    return 'image';
  }
  if (const {
    '.mp3',
    '.wav',
    '.m4a',
    '.aac',
    '.ogg',
    '.opus',
    '.flac',
  }.contains(extension)) {
    return 'audio';
  }
  if (const {'.mp4', '.webm', '.mov', '.m4v'}.contains(extension)) {
    return 'video';
  }
  return 'file';
}

String _attachmentName(String url) {
  final uri = Uri.tryParse(url);
  final segments = uri?.pathSegments ?? const <String>[];
  return segments.isEmpty ? 'Attachment' : segments.last;
}
