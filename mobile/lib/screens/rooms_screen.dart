import 'package:flutter/material.dart';

import '../api.dart';
import '../app_state.dart';
import '../format.dart';
import '../models.dart';
import '../realtime.dart';
import '../responsive.dart';
import '../theme.dart';
import '../widgets.dart';
import 'profile_screen.dart';
import 'room_chat_screen.dart';

class RoomsScreen extends StatefulWidget {
  const RoomsScreen({super.key});

  @override
  State<RoomsScreen> createState() => _RoomsScreenState();
}

class _RoomsScreenState extends State<RoomsScreen> {
  static const _filters = ['open', 'closed', 'all'];
  static const _labels = ['Open', 'Closed', 'Latest'];

  int _filter = 0;
  List<Room> _rooms = const [];
  List<FeedItem> _feed = const [];
  bool _loading = true;
  int _request = 0;
  VoidCallback? _unbind;

  Room? get _mainRoom => _rooms.isEmpty ? null : _rooms.first;

  @override
  void initState() {
    super.initState();
    _load();
    _unbind = realtime.bind({AtgScope.rooms}, _load);
  }

  @override
  void dispose() {
    _unbind?.call();
    super.dispose();
  }

  Future<void> _load() async {
    final request = ++_request;
    setState(() => _loading = true);
    try {
      final results = await Future.wait([
        api.rooms(),
        api.feed(_filters[_filter]),
      ]);
      if (!mounted || request != _request) return;
      setState(() {
        _rooms = results[0] as List<Room>;
        _feed = results[1] as List<FeedItem>;
        _loading = false;
      });
    } on ApiException catch (e) {
      if (!mounted || request != _request) return;
      setState(() => _loading = false);
      showToast(context, e.message);
    }
  }

  void _openRoom(Room room) {
    if (room.locked) {
      showMessage(
        context,
        room.name,
        '${membershipLabel(room.requiredMembership)} members only. Buy the membership from your profile, '
        'attach your payment proof, and the administrator unlocks the room after reviewing it.',
      );
      return;
    }
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => RoomChatScreen(roomId: room.id, roomName: room.name),
      ),
    );
  }

  void _openFeedItem(FeedItem item) {
    final roomId = item.roomId > 0 ? item.roomId : (_mainRoom?.id ?? 0);
    if (roomId <= 0) return;
    Room? room;
    for (final r in _rooms) {
      if (r.id == roomId) {
        room = r;
        break;
      }
    }
    if (room != null) {
      _openRoom(room);
      return;
    }
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => RoomChatScreen(
          roomId: roomId,
          roomName: item.roomName.isEmpty ? 'Room' : item.roomName,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final mainRoom = _mainRoom;

    return SafeArea(
      bottom: false,
      child: PageBody(
        maxWidth: 980,
        child: Column(
          children: [
            TabHeader(
              title: 'Community',
              subtitle: 'Live conversation from the desk',
              onBell: () => notifyAllCaughtUp(context),
              onAvatar: () => Navigator.of(
                context,
              ).push(MaterialPageRoute(builder: (_) => const ProfileScreen())),
            ),
            if (mainRoom != null)
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 2, 16, 0),
                child: SurfaceCard(
                  padding: const EdgeInsets.all(14),
                  onTap: () => _openRoom(mainRoom),
                  child: Row(
                    children: [
                      const AtgLogo(size: 44),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              mainRoom.name,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: AtgText.h2,
                            ),
                            const SizedBox(height: 2),
                            Text(
                              mainRoom.preview.isEmpty
                                  ? 'Tap to open the room'
                                  : mainRoom.preview,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: AtgText.small,
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 8),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          Text(
                            timeAgo(mainRoom.lastActivityAt),
                            style: AtgText.tiny,
                          ),
                          const SizedBox(height: 6),
                          AtgPill(
                            text: mainRoom.unreadCount >= 99
                                ? '99+'
                                : '${mainRoom.unreadCount}',
                            background: AtgColors.primarySoft,
                            foreground: AtgColors.gold,
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 14, 16, 8),
              child: AtgChipRow(
                labels: _labels,
                selected: _filter,
                onSelected: (index) {
                  if (index == _filter) return;
                  setState(() => _filter = index);
                  _load();
                },
              ),
            ),
            const Padding(
              padding: EdgeInsets.fromLTRB(16, 4, 16, 0),
              child: Row(
                children: [
                  Text(
                    'Signal',
                    style: TextStyle(fontSize: 13, color: AtgColors.gold),
                  ),
                  Spacer(),
                  Text(
                    'Latest activity',
                    style: TextStyle(fontSize: 13, color: AtgColors.gold),
                  ),
                ],
              ),
            ),
            Expanded(
              child: RefreshIndicator(
                color: AtgColors.primary,
                backgroundColor: AtgColors.surface,
                onRefresh: _load,
                child: _loading && _feed.isEmpty
                    ? ListView(
                        physics: const AlwaysScrollableScrollPhysics(),
                        children: const [LoadingBlock()],
                      )
                    : _feed.isEmpty
                    ? ListView(
                        physics: const AlwaysScrollableScrollPhysics(),
                        padding: const EdgeInsets.only(top: 30),
                        children: const [
                          EmptyState(
                            title: 'No activity',
                            message: 'Traders are quiet. New posts appear here first.',
                            icon: Icons.forum_outlined,
                          ),
                        ],
                      )
                    : ListView.builder(
                        physics: const AlwaysScrollableScrollPhysics(),
                        padding: const EdgeInsets.fromLTRB(16, 8, 16, 100),
                        itemCount: _feed.length,
                        itemBuilder: (context, index) {
                          final item = _feed[index];
                          return FeedRow(
                            item: item,
                            onTap: () => _openFeedItem(item),
                          );
                        },
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
