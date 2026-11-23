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
import 'profile_screen.dart';
import 'room_chat_screen.dart';

class RoomsScreen extends StatefulWidget {
  const RoomsScreen({super.key});

  @override
  State<RoomsScreen> createState() => _RoomsScreenState();
}

class _RoomsScreenState extends State<RoomsScreen> {
  static const _filters = ['open', 'closed', 'latest'];
  static const _labels = ['Open', 'Closed', 'Latest'];

  int _filter = 0;
  List<Room> _rooms = const [];
  List<FeedItem> _feed = const [];
  bool _loading = true;
  String? _roomsError;
  String? _feedError;
  int _request = 0;
  VoidCallback? _unbind;

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
    setState(() {
      _loading = true;
      _roomsError = null;
      _feedError = null;
    });
    final roomsFuture = api.rooms();
    final feedFuture = api.feed(_filters[_filter]);
    try {
      final rooms = await roomsFuture;
      if (!mounted || request != _request) return;
      setState(() {
        _rooms = rooms;
      });
    } on ApiException catch (e) {
      if (!mounted || request != _request) return;
      setState(() => _roomsError = e.message);
    } catch (error, stackTrace) {
      debugPrint('Could not load chat rooms: $error\n$stackTrace');
      if (!mounted || request != _request) return;
      setState(() => _roomsError = 'Could not load chat rooms. Try again.');
    }

    try {
      final feed = await feedFuture;
      if (!mounted || request != _request) return;
      setState(() {
        _feed = feed;
      });
    } on ApiException catch (e) {
      if (!mounted || request != _request) return;
      setState(() => _feedError = e.message);
    } catch (error, stackTrace) {
      debugPrint('Could not load community activity: $error\n$stackTrace');
      if (!mounted || request != _request) return;
      setState(
        () => _feedError = 'Could not load community activity. Try again.',
      );
    } finally {
      if (mounted && request == _request) {
        setState(() => _loading = false);
      }
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
    final roomId = item.roomId > 0
        ? item.roomId
        : (_rooms.isEmpty ? 0 : _rooms.first.id);
    if (roomId <= 0) return;
    Room? room;
    for (final r in _rooms) {
      if (r.id == roomId) {
        room = r;
        break;
      }
    }
    if (room != null && room.locked) {
      _openRoom(room);
      return;
    }
    if (item.createdById == null) {
      if (room != null) {
        _openRoom(room);
      } else {
        final roomName = item.roomName.isEmpty ? 'Community' : item.roomName;
        Navigator.of(context).push(
          MaterialPageRoute(
            builder: (_) => RoomChatScreen(roomId: roomId, roomName: roomName),
          ),
        );
      }
      return;
    }
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => RoomChatScreen(
          roomId: roomId,
          roomName: item.roomName.isEmpty ? 'Community' : item.roomName,
          threadId: item.id,
          threadTitle: item.title,
          threadOpen: item.open,
          canManageThread: session.isAdmin || item.createdById == session.id,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
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
            if (_rooms.isEmpty)
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 2, 16, 0),
                child: SurfaceCard(
                  padding: const EdgeInsets.all(14),
                  child: Row(
                    children: [
                      const AtgLogo(size: 40),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          _roomsError ??
                              (_loading
                                  ? 'Loading chat rooms…'
                                  : 'Community will be set up automatically.'),
                          style: AtgText.bodySmall,
                        ),
                      ),
                      if (_roomsError != null)
                        IconButton(
                          tooltip: 'Retry loading rooms',
                          onPressed: _load,
                          icon: const Icon(Icons.refresh_rounded),
                        ),
                    ],
                  ),
                ),
              )
            else
              SizedBox(
                height: 94,
                child: ListView.separated(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  itemCount: _rooms.length,
                  separatorBuilder: (_, _) => const SizedBox(width: 8),
                  itemBuilder: (context, index) {
                    final room = _rooms[index];
                    return SizedBox(
                      width: 250,
                      child: SurfaceCard(
                        padding: const EdgeInsets.all(12),
                        onTap: () => _openRoom(room),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Expanded(
                                  child: Text(
                                    room.name,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: AtgText.h3,
                                  ),
                                ),
                                if (room.locked)
                                  Icon(
                                    Icons.lock_outline_rounded,
                                    size: 15,
                                    color: AtgColors.gold,
                                  )
                                else
                                  Text(
                                    timeAgo(room.lastActivityAt),
                                    style: AtgText.tiny,
                                  ),
                              ],
                            ),
                            const SizedBox(height: 5),
                            Expanded(
                              child: LinkedText(
                                room.preview.isNotEmpty
                                    ? room.preview
                                    : room.description.isNotEmpty
                                    ? room.description
                                    : 'Tap to start chatting',
                                style: AtgText.small,
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ],
                        ),
                      ),
                    );
                  },
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
            Padding(
              padding: EdgeInsets.fromLTRB(16, 4, 16, 0),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  'Community conversations',
                  style: TextStyle(fontSize: 13, color: AtgColors.gold),
                ),
              ),
            ),
            Expanded(
              child: RefreshIndicator(
                color: AtgColors.primary,
                backgroundColor: Theme.of(context).colorScheme.surface,
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
                        children: [
                          EmptyState(
                            title: _feedError == null
                                ? 'No conversations yet'
                                : 'Could not load activity',
                            message:
                                _feedError ??
                                'Open Community to start chatting.',
                            icon: _feedError == null
                                ? Icons.forum_outlined
                                : Icons.cloud_off_outlined,
                          ),
                          if (_feedError != null)
                            Padding(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 24,
                              ),
                              child: OutlineButton(
                                label: 'Try again',
                                onPressed: _load,
                              ),
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
