import 'package:flutter/material.dart';

import '../api.dart';
import '../app_state.dart';
import '../format.dart';
import '../models.dart';
import '../realtime.dart';
import '../responsive.dart';
import '../theme.dart';
import '../widgets.dart';
import 'course_detail_screen.dart';

class AcademyScreen extends StatefulWidget {
  const AcademyScreen({super.key});

  @override
  State<AcademyScreen> createState() => _AcademyScreenState();
}

class _AcademyScreenState extends State<AcademyScreen> {
  List<CourseRow> _rows = const [];
  bool _loading = true;
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
      final rows = await api.courses();
      if (!mounted) return;
      setState(() {
        _rows = rows;
        _loading = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _loading = false);
      showToast(context, e.message);
    }
  }

  Future<void> _open(CourseRow row) async {
    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => CourseDetailScreen(courseId: row.course.id),
      ),
    );
    if (mounted) await _load();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AtgColors.bg,
      body: SafeArea(
        bottom: false,
        child: PageBody(
          maxWidth: 980,
          child: Column(
            children: [
              const DetailHeader(title: 'Academy'),
              Expanded(
                child: RefreshIndicator(
                  color: AtgColors.primary,
                  backgroundColor: AtgColors.surface,
                  onRefresh: _load,
                  child: _loading && _rows.isEmpty
                      ? ListView(
                          physics: const AlwaysScrollableScrollPhysics(),
                          children: const [LoadingBlock()],
                        )
                      : _rows.isEmpty
                      ? ListView(
                          physics: const AlwaysScrollableScrollPhysics(),
                          padding: const EdgeInsets.only(top: 30),
                          children: const [
                            EmptyState(
                              title: 'No courses yet',
                              message: 'The desk is preparing the next course. Check back soon.',
                              icon: Icons.school_outlined,
                            ),
                          ],
                        )
                      : ListView.builder(
                          physics: const AlwaysScrollableScrollPhysics(),
                          padding: const EdgeInsets.fromLTRB(16, 4, 16, 28),
                          itemCount: _rows.length,
                          itemBuilder: (context, index) {
                            final row = _rows[index];
                            return Padding(
                              padding: const EdgeInsets.only(bottom: 12),
                              child: _CourseCard(
                                row: row,
                                onTap: () => _open(row),
                              ),
                            );
                          },
                        ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _CourseCard extends StatelessWidget {
  const _CourseCard({required this.row, required this.onTap});

  final CourseRow row;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final course = row.course;
    return SurfaceCard(
      onTap: onTap,
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
              const Spacer(),
              if (row.enrolled)
                const AtgPill(
                  text: 'ENROLLED',
                  background: AtgColors.greenBg,
                  foreground: AtgColors.greenText,
                )
              else
                Text(
                  money(course.price),
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w800,
                    color: AtgColors.ink,
                  ),
                ),
            ],
          ),
          const SizedBox(height: 12),
          Text(course.title, style: AtgText.h1),
          if (course.subtitle.isNotEmpty) ...[
            const SizedBox(height: 3),
            Text(
              course.subtitle,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: AtgText.small,
            ),
          ],
          if (row.enrolled) ...[
            const SizedBox(height: 14),
            ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value: (row.progressPct.clamp(0, 100)) / 100,
                minHeight: 6,
                backgroundColor: AtgColors.strokeSoft,
                valueColor: const AlwaysStoppedAnimation<Color>(
                  AtgColors.primary,
                ),
              ),
            ),
            const SizedBox(height: 6),
            Text(
              '${row.completedLessons} of ${row.totalLessons} lessons complete',
              style: AtgText.small.copyWith(fontSize: 11.5),
            ),
          ] else ...[
            const SizedBox(height: 12),
            Row(
              children: [
                Text(
                  'Buy to unlock every lesson',
                  style: AtgText.small.copyWith(color: AtgColors.gold),
                ),
                const Spacer(),
                const Icon(
                  Icons.chevron_right_rounded,
                  size: 20,
                  color: AtgColors.muted,
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}
