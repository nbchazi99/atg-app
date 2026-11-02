import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher.dart';

import '../api.dart';
import '../app_state.dart';
import '../models.dart';
import '../responsive.dart';
import '../theme.dart';
import '../widgets.dart';

class LessonScreen extends StatefulWidget {
  const LessonScreen({
    super.key,
    required this.courseId,
    required this.lesson,
    required this.completed,
  });

  final int courseId;
  final Lesson lesson;
  final bool completed;

  @override
  State<LessonScreen> createState() => _LessonScreenState();
}

class _LessonScreenState extends State<LessonScreen> {
  late bool _completed = widget.completed;
  bool _saving = false;
  bool _sharing = false;

  Future<void> _markComplete() async {
    if (_completed || _saving) return;
    setState(() => _saving = true);
    try {
      await api.completeLesson(widget.courseId, widget.lesson.id);
      if (!mounted) return;
      setState(() {
        _completed = true;
        _saving = false;
      });
      showToast(context, 'Lesson complete');
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _saving = false);
      showToast(context, e.message);
    }
  }

  Future<void> _watchVideo() async {
    final url = widget.lesson.videoUrl.trim();
    if (url.isEmpty) return;
    final ok = await launchUrl(
      Uri.parse(url),
      mode: LaunchMode.externalApplication,
    );
    if (!ok && mounted) showToast(context, 'No app can open that video link');
  }

  String get _notesFileName {
    final base = widget.lesson.title
        .toLowerCase()
        .replaceAll(RegExp(r'[^a-z0-9]+'), '-')
        .replaceAll(RegExp(r'^-+|-+$'), '');
    final trimmed = base.isEmpty
        ? 'lesson'
        : (base.length > 40 ? base.substring(0, 40) : base);
    return '$trimmed-notes.txt';
  }

  Future<void> _shareNotes() async {
    if (_sharing) return;
    setState(() => _sharing = true);
    try {
      final lesson = widget.lesson;
      final body =
          '${lesson.title}\n'
          '${lesson.durationMin} min · Lesson ${lesson.orderIndex + 1}\n\n'
          '${lesson.content}\n\n'
          '— ATG · Accurate Trading Group\n';
      final dir = await getTemporaryDirectory();
      final file = File('${dir.path}/$_notesFileName');
      await file.writeAsString(body, encoding: utf8);
      await SharePlus.instance.share(
        ShareParams(
          files: [XFile(file.path, mimeType: 'text/plain')],
          subject: '${lesson.title} — ATG notes',
        ),
      );
    } catch (_) {
      if (mounted) showToast(context, 'Could not save the notes');
    } finally {
      if (mounted) setState(() => _sharing = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final lesson = widget.lesson;

    return Scaffold(
      backgroundColor: AtgColors.bg,
      body: SafeArea(
        bottom: false,
        child: PageBody(
          maxWidth: 760,
          child: Column(
            children: [
              const DetailHeader(title: 'Lesson'),
              Expanded(
                child: ListView(
                  padding: const EdgeInsets.fromLTRB(16, 4, 16, 28),
                  children: [
                    SectionLabel(
                      '${lesson.durationMin} MIN · LESSON ${lesson.orderIndex + 1}',
                    ),
                    const SizedBox(height: 8),
                    Text(lesson.title, style: AtgText.pageTitle),
                    const SizedBox(height: 16),
                    SurfaceCard(
                      child: Text(
                        lesson.content.trim().isEmpty
                            ? 'The desk has not added notes for this lesson yet.'
                            : lesson.content,
                        style: AtgText.body.copyWith(height: 1.65),
                      ),
                    ),
                    if (lesson.videoUrl.trim().isNotEmpty) ...[
                      const SizedBox(height: 14),
                      OutlineButton(
                        label: 'Watch the video',
                        icon: Icons.play_circle_outline_rounded,
                        onPressed: _watchVideo,
                      ),
                    ],
                    const SizedBox(height: 14),
                    OutlineButton(
                      label: _sharing ? 'Preparing notes…' : 'Save these notes',
                      icon: Icons.ios_share_rounded,
                      onPressed: _sharing ? null : _shareNotes,
                    ),
                    const SizedBox(height: 14),
                    _CompletedButton(
                      completed: _completed,
                      saving: _saving,
                      onPressed: _markComplete,
                    ),
                    const SizedBox(height: 10),
                    const Text(
                      'Progress is saved to your ATG account, so it follows you to the website and any other phone.',
                      style: TextStyle(
                        fontSize: 11,
                        color: AtgColors.muted,
                        height: 1.45,
                      ),
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
}

class _CompletedButton extends StatelessWidget {
  const _CompletedButton({
    required this.completed,
    required this.saving,
    required this.onPressed,
  });

  final bool completed;
  final bool saving;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    if (saving) {
      return const PrimaryButton(
        label: 'Saving…',
        loading: true,
        onPressed: null,
      );
    }
    if (completed) {
      return SizedBox(
        height: 54,
        width: double.infinity,
        child: FilledButton(
          onPressed: null,
          style: FilledButton.styleFrom(
            backgroundColor: AtgColors.primarySoft,
            disabledBackgroundColor: AtgColors.primarySoft,
            disabledForegroundColor: AtgColors.gold,
            elevation: 0,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(14),
            ),
            textStyle: const TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.w800,
            ),
          ),
          child: const Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.check_circle_rounded, size: 19),
              SizedBox(width: 8),
              Text('Completed'),
            ],
          ),
        ),
      );
    }
    return PrimaryButton(label: 'Mark as complete', onPressed: onPressed);
  }
}
