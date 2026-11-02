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
import 'lesson_screen.dart';

class CourseDetailScreen extends StatefulWidget {
  const CourseDetailScreen({super.key, required this.courseId});

  final int courseId;

  @override
  State<CourseDetailScreen> createState() => _CourseDetailScreenState();
}

class _CourseDetailScreenState extends State<CourseDetailScreen> {
  CourseDetail? _detail;
  bool _loading = true;
  bool _buying = false;
  String? _error;
  VoidCallback? _unbind;

  @override
  void initState() {
    super.initState();
    _load();
    _unbind = realtime.bind({AtgScope.courses}, _load);
  }

  @override
  void dispose() {
    _unbind?.call();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final detail = await api.course(widget.courseId);
      if (!mounted) return;
      setState(() {
        _detail = detail;
        _loading = false;
        _error = null;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = e.message;
      });
    }
  }

  Future<void> _buy(Course course) async {
    if (_buying) return;
    final proceed = await confirmAction(
      context,
      title: 'Buy ${course.title}',
      message:
          'Pay ${money(course.price)} to the ATG desk, then attach a screenshot of the payment. '
          'The administrator unlocks the course after reviewing it.',
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

    setState(() => _buying = true);
    try {
      final url = await api.uploadImage(proof);
      final purchase = await api.purchase(
        'COURSE',
        courseId: course.id,
        proofUrl: url,
      );
      if (!mounted) return;
      setState(() => _buying = false);
      await showMessage(
        context,
        'Purchase filed',
        'Reference P-${purchase.id}. The administrator reviews proofs manually; once approved the lessons unlock here.',
        action: 'Done',
      );
      if (mounted) await _load();
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _buying = false);
      showMessage(context, 'Could not file the purchase', e.message);
    }
  }

  Future<void> _openLesson(Lesson lesson, bool completed) async {
    final detail = _detail;
    if (detail == null) return;
    if (!detail.enrolled) {
      showToast(context, 'Buy this course to unlock the lessons');
      return;
    }
    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => LessonScreen(
          courseId: detail.course.id,
          lesson: lesson,
          completed: completed,
        ),
      ),
    );
    if (mounted) await _load();
  }

  @override
  Widget build(BuildContext context) {
    final detail = _detail;

    return Scaffold(
      backgroundColor: AtgColors.bg,
      body: SafeArea(
        bottom: false,
        child: PageBody(
          maxWidth: 760,
          child: Column(
            children: [
              const DetailHeader(title: 'Course'),
              Expanded(
                child: _loading && detail == null
                    ? const LoadingBlock()
                    : detail == null
                    ? ListView(
                        padding: const EdgeInsets.only(top: 30),
                        children: [
                          EmptyState(
                            title: 'Could not load this course',
                            message: _error ?? 'Try again in a moment.',
                            icon: Icons.cloud_off_outlined,
                          ),
                          Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 24),
                            child: OutlineButton(
                              label: 'Try again',
                              onPressed: _load,
                            ),
                          ),
                        ],
                      )
                    : ListView(
                        padding: const EdgeInsets.fromLTRB(16, 4, 16, 28),
                        children: [
                          _buildIntro(detail),
                          if (detail.enrolled) ...[
                            const SizedBox(height: 14),
                            _buildProgress(detail),
                          ],
                          const SizedBox(height: 22),
                          const SectionLabel('LESSONS'),
                          const SizedBox(height: 10),
                          if (detail.lessons.isEmpty)
                            const EmptyState(
                              title: 'Lessons coming',
                              message: 'The desk is still writing this course.',
                              icon: Icons.menu_book_outlined,
                            )
                          else
                            for (final lesson in detail.lessons)
                              _LessonTile(
                                lesson: lesson,
                                completed: detail.completedLessonIds.contains(
                                  lesson.id,
                                ),
                                onTap: () => _openLesson(
                                  lesson,
                                  detail.completedLessonIds.contains(lesson.id),
                                ),
                              ),
                          if (!detail.enrolled) ...[
                            const SizedBox(height: 16),
                            SurfaceCard(
                              padding: const EdgeInsets.all(14),
                              child: Row(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  const Icon(
                                    Icons.lock_outline_rounded,
                                    size: 18,
                                    color: AtgColors.gold,
                                  ),
                                  const SizedBox(width: 8),
                                  const Expanded(
                                    child: Text(
                                      'Lessons unlock after the administrator approves your payment proof. '
                                      'You will see the decision in your profile.',
                                      style: AtgText.small,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            const SizedBox(height: 14),
                            PrimaryButton(
                              label:
                                  'Buy this course · ${money(detail.course.price)}',
                              loading: _buying,
                              onPressed: () => _buy(detail.course),
                            ),
                          ],
                        ],
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildIntro(CourseDetail detail) {
    final course = detail.course;
    return SurfaceCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (course.imageUrl.isNotEmpty) ...[
            CourseCover(url: course.imageUrl),
            const SizedBox(height: 12),
          ],
          Row(
            children: [
              AtgPill(
                text: course.level.toUpperCase(),
                background: AtgColors.primarySoft,
                foreground: AtgColors.gold,
              ),
              const SizedBox(width: 6),
              AtgPill(text: '${course.lessonsCount} LESSONS'),
              if (detail.enrolled) ...[
                const SizedBox(width: 6),
                const AtgPill(
                  text: 'ENROLLED',
                  background: AtgColors.greenBg,
                  foreground: AtgColors.greenText,
                ),
              ],
            ],
          ),
          const SizedBox(height: 12),
          Text(course.title, style: AtgText.pageTitle.copyWith(fontSize: 22)),
          if (course.subtitle.isNotEmpty) ...[
            const SizedBox(height: 4),
            Text(course.subtitle, style: AtgText.body),
          ],
          if (course.description.isNotEmpty) ...[
            const SizedBox(height: 12),
            Text(course.description, style: AtgText.bodySmall),
          ],
          const SizedBox(height: 14),
          Row(
            children: [
              Text('Course price', style: AtgText.small),
              const Spacer(),
              Text(
                money(course.price),
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w800,
                  color: AtgColors.ink,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildProgress(CourseDetail detail) {
    final total = detail.course.lessonsCount;
    final done = detail.completedLessonIds.length;
    final pct = total == 0 ? 0 : ((done * 100) / total).round();
    return SurfaceCard(
      padding: const EdgeInsets.all(15),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('$done of $total lessons complete', style: AtgText.h3),
          const SizedBox(height: 10),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: (pct.clamp(0, 100)) / 100,
              minHeight: 7,
              backgroundColor: AtgColors.strokeSoft,
              valueColor: const AlwaysStoppedAnimation<Color>(
                AtgColors.primary,
              ),
            ),
          ),
          const SizedBox(height: 6),
          Text('$pct% complete', style: AtgText.small.copyWith(fontSize: 11.5)),
        ],
      ),
    );
  }
}

class _LessonTile extends StatelessWidget {
  const _LessonTile({
    required this.lesson,
    required this.completed,
    required this.onTap,
  });

  final Lesson lesson;
  final bool completed;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: SurfaceCard(
        padding: const EdgeInsets.all(13),
        onTap: onTap,
        child: Row(
          children: [
            Container(
              width: 38,
              height: 38,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: completed ? AtgColors.greenBg : AtgColors.primarySoft,
                shape: BoxShape.circle,
              ),
              child: completed
                  ? const Icon(
                      Icons.check_rounded,
                      size: 18,
                      color: AtgColors.greenText,
                    )
                  : Text(
                      '${lesson.orderIndex + 1}',
                      style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w800,
                        color: AtgColors.gold,
                      ),
                    ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    lesson.title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: AtgText.h3,
                  ),
                  const SizedBox(height: 2),
                  Text(
                    '${lesson.durationMin} min${completed ? ' · Completed' : ''}',
                    style: AtgText.small.copyWith(
                      fontSize: 11.5,
                      color: completed ? AtgColors.greenText : AtgColors.muted,
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
  }
}
