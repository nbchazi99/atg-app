import 'package:flutter/material.dart';

import '../api.dart';
import '../app_state.dart';
import '../format.dart';
import '../linked_text.dart';
import '../models.dart';
import '../realtime.dart';
import '../theme.dart';
import '../widgets.dart';

class AcademyDiscussion extends StatefulWidget {
  const AcademyDiscussion({
    super.key,
    required this.courseId,
    this.lessonId,
    required this.title,
  });

  final int courseId;
  final int? lessonId;
  final String title;

  @override
  State<AcademyDiscussion> createState() => _AcademyDiscussionState();
}

class _AcademyDiscussionState extends State<AcademyDiscussion> {
  final _input = TextEditingController();
  List<AcademyComment> _comments = const [];
  bool _loading = true;
  bool _sending = false;
  String? _error;
  VoidCallback? _unbind;

  @override
  void initState() {
    super.initState();
    _load();
    _unbind = realtime.bind({AtgScope.courses}, () => _load(silent: true));
  }

  @override
  void dispose() {
    _unbind?.call();
    _input.dispose();
    super.dispose();
  }

  Future<void> _load({bool silent = false}) async {
    try {
      final comments = await api.academyComments(
        widget.courseId,
        lessonId: widget.lessonId,
      );
      if (!mounted) return;
      setState(() {
        _comments = comments;
        _error = null;
        _loading = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      if (!silent || _comments.isEmpty) {
        setState(() {
          _error = e.message;
          _loading = false;
        });
      }
    } catch (error, stackTrace) {
      debugPrint('Could not load Academy comments: $error\n$stackTrace');
      if (!mounted || (silent && _comments.isNotEmpty)) return;
      setState(() {
        _error = 'Could not load comments. Try again.';
        _loading = false;
      });
    }
  }

  Future<void> _send() async {
    final text = _input.text.trim();
    if (text.isEmpty || _sending) return;
    setState(() => _sending = true);
    try {
      final comment = await api.addAcademyComment(
        widget.courseId,
        text,
        lessonId: widget.lessonId,
      );
      if (!mounted) return;
      _input.clear();
      setState(() {
        _comments = [..._comments, comment];
        _error = null;
      });
    } on ApiException catch (e) {
      if (mounted) showToast(context, e.message);
    } catch (error, stackTrace) {
      debugPrint('Could not post Academy comment: $error\n$stackTrace');
      if (mounted) showToast(context, 'Could not post comment. Try again.');
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SectionLabel(widget.title.toUpperCase()),
        const SizedBox(height: 10),
        SurfaceCard(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Expanded(
                    child: TextField(
                      controller: _input,
                      minLines: 1,
                      maxLines: 4,
                      maxLength: 2000,
                      textCapitalization: TextCapitalization.sentences,
                      textInputAction: TextInputAction.newline,
                      style: const TextStyle(
                        fontSize: 14,
                        color: AtgColors.ink,
                      ),
                      decoration: atgInputDecoration(
                        hint: 'Share a comment or link',
                      ).copyWith(counterText: ''),
                    ),
                  ),
                  const SizedBox(width: 8),
                  IconButton.filled(
                    onPressed: _sending ? null : _send,
                    tooltip: 'Post comment',
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
              if (_loading && _comments.isEmpty)
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 14),
                  child: Center(
                    child: CircularProgressIndicator(strokeWidth: 2),
                  ),
                )
              else if (_error != null && _comments.isEmpty)
                Row(
                  children: [
                    Expanded(child: Text(_error!, style: AtgText.small)),
                    TextButton(onPressed: _load, child: const Text('Retry')),
                  ],
                )
              else if (_comments.isEmpty)
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 8),
                  child: Text(
                    'No comments yet. Start the discussion.',
                    style: AtgText.small,
                  ),
                )
              else
                for (final comment in _comments) ...[
                  const Divider(height: 18),
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          comment.authorName,
                          style: const TextStyle(
                            color: AtgColors.gold,
                            fontSize: 12,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                      ),
                      Text(timeAgo(comment.createdAt), style: AtgText.tiny),
                    ],
                  ),
                  const SizedBox(height: 4),
                  LinkedText(comment.text, style: AtgText.bodySmall),
                ],
            ],
          ),
        ),
      ],
    );
  }
}
