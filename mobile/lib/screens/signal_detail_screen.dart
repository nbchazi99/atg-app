import 'package:flutter/material.dart';

import '../api.dart';
import '../app_state.dart';
import '../format.dart';
import '../models.dart';
import '../responsive.dart';
import '../theme.dart';
import '../widgets.dart';
import 'room_chat_screen.dart';

class SignalDetailScreen extends StatefulWidget {
  const SignalDetailScreen({super.key, required this.signal});

  final Signal signal;

  @override
  State<SignalDetailScreen> createState() => _SignalDetailScreenState();
}

class _SignalDetailScreenState extends State<SignalDetailScreen> {
  bool _opening = false;

  Future<void> _openRoom() async {
    if (_opening) return;
    setState(() => _opening = true);
    try {
      final rooms = await api.rooms();
      if (!mounted) return;
      setState(() => _opening = false);
      if (rooms.isEmpty) {
        showMessage(
          context,
          'Community',
          'The community is not open yet. Check back shortly.',
        );
        return;
      }
      Room? target;
      final label = widget.signal.roomLabel.toLowerCase();
      for (final room in rooms) {
        if (label.isNotEmpty && room.name.toLowerCase().contains(label)) {
          target = room;
          break;
        }
      }
      target ??= rooms.first;
      if (target.locked) {
        showMessage(
          context,
          target.name,
          '${membershipLabel(target.requiredMembership)} members only. Buy the membership from your profile, '
          'attach your payment proof, and the administrator unlocks the room after reviewing it.',
        );
        return;
      }
      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (_) =>
              RoomChatScreen(roomId: target!.id, roomName: target.name),
        ),
      );
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _opening = false);
      showToast(context, e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    final signal = widget.signal;

    return Scaffold(
      backgroundColor: AtgColors.bg,
      body: SafeArea(
        bottom: false,
        child: PageBody(
          maxWidth: 760,
          child: Column(
            children: [
              const _SignalHeader(),
              Expanded(
                child: ListView(
                  padding: const EdgeInsets.fromLTRB(16, 4, 16, 28),
                  children: [
                    SurfaceCard(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              SidePill(signal.side, fontSize: 11),
                              const SizedBox(width: 6),
                              StatusPill(signal.status, fontSize: 10),
                              if (signal.isLive) ...[
                                const SizedBox(width: 6),
                                const LivePill(),
                              ],
                              const Spacer(),
                              AtgPill(
                                text: signal.roomLabel.toUpperCase(),
                                background: AtgColors.primarySoft,
                                foreground: AtgColors.gold,
                                fontSize: 9.5,
                              ),
                            ],
                          ),
                          const SizedBox(height: 14),
                          Text(
                            signal.title,
                            style: AtgText.pageTitle.copyWith(fontSize: 22),
                          ),
                          if (signal.subtitle.isNotEmpty) ...[
                            const SizedBox(height: 4),
                            Text(signal.subtitle, style: AtgText.body),
                          ],
                          const SizedBox(height: 16),
                          const Divider(),
                          const SizedBox(height: 14),
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Expanded(
                                child: _Level(
                                  label: 'ENTRY',
                                  value: signal.entry,
                                  fallback: 'At market, now',
                                ),
                              ),
                              const _LevelDivider(),
                              Expanded(
                                child: _Level(
                                  label: 'STOP LOSS',
                                  value: signal.stopLoss,
                                  fallback: 'Not given',
                                ),
                              ),
                              const _LevelDivider(),
                              Expanded(
                                child: _Level(
                                  label: 'TARGETS',
                                  value: signal.targets,
                                  fallback: 'Not given',
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 16),
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 12,
                              vertical: 10,
                            ),
                            decoration: BoxDecoration(
                              color: AtgColors.surfaceAlt,
                              borderRadius: BorderRadius.circular(12),
                              border: Border.all(color: AtgColors.strokeSoft),
                            ),
                            child: Row(
                              children: [
                                const Icon(
                                  Icons.gavel_outlined,
                                  size: 16,
                                  color: AtgColors.gold,
                                ),
                                const SizedBox(width: 8),
                                Expanded(
                                  child: Text(
                                    signal.ruleName.isEmpty
                                        ? 'M10 rule'
                                        : signal.ruleName,
                                    style: AtgText.h3.copyWith(fontSize: 13),
                                  ),
                                ),
                                if (signal.symbol.isNotEmpty)
                                  Text(
                                    displaySymbol(signal.symbol),
                                    style: AtgText.small,
                                  ),
                              ],
                            ),
                          ),
                          const SizedBox(height: 14),
                          Row(
                            children: [
                              const Icon(
                                Icons.people_outline_rounded,
                                size: 15,
                                color: AtgColors.muted,
                              ),
                              const SizedBox(width: 5),
                              Text(
                                signal.participants == 1
                                    ? '1 trader'
                                    : '${signal.participants} traders',
                                style: AtgText.small,
                              ),
                              const Spacer(),
                              Text(
                                timeAgo(signal.createdAt),
                                style: AtgText.small,
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 14),
                    SurfaceCard(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const SectionLabel('HOW TO TRADE THIS CALL'),
                          const SizedBox(height: 8),
                          Text(
                            signal.isBuy
                                ? 'Wait for the candle to close in your favour before entering. Risk no more than 1% of '
                                      'your account. Move the stop to break-even when the first target prints, and do not '
                                      'add to a losing position.'
                                : 'Sell only after price rejects the level on the close. Risk no more than 1% of your '
                                      'account, trail the stop behind structure, and take the call off the table if the rule '
                                      'invalidates.',
                            style: AtgText.bodySmall,
                          ),
                          const SizedBox(height: 12),
                          const Text(
                            'Education and signals only. No financial advice.',
                            style: TextStyle(
                              fontSize: 10.5,
                              color: AtgColors.muted,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 18),
                    PrimaryButton(
                      label: 'Open the room chat',
                      icon: Icons.forum_outlined,
                      loading: _opening,
                      onPressed: _openRoom,
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

class _SignalHeader extends StatelessWidget {
  const _SignalHeader();

  @override
  Widget build(BuildContext context) {
    return const DetailHeader(title: 'Signal');
  }
}

class _Level extends StatelessWidget {
  const _Level({
    required this.label,
    required this.value,
    required this.fallback,
  });

  final String label;
  final String value;
  final String fallback;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: AtgText.label.copyWith(letterSpacing: 1.2)),
        const SizedBox(height: 4),
        Text(
          value.trim().isEmpty ? fallback : value,
          maxLines: 3,
          overflow: TextOverflow.ellipsis,
          style: const TextStyle(
            fontSize: 13.5,
            color: AtgColors.ink,
            height: 1.35,
            fontWeight: FontWeight.w600,
          ),
        ),
      ],
    );
  }
}

class _LevelDivider extends StatelessWidget {
  const _LevelDivider();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 1,
      height: 46,
      margin: const EdgeInsets.only(left: 10, right: 12),
      color: AtgColors.strokeSoft,
    );
  }
}
