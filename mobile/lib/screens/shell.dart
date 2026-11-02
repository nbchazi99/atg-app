import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

import '../app_state.dart';
import '../realtime.dart';
import '../theme.dart';
import 'home_screen.dart';
import 'markets_screen.dart';
import 'rooms_screen.dart';
import 'signals_screen.dart';
import 'wallet_screen.dart';

class AppShell extends StatefulWidget {
  const AppShell({super.key});

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  int _index = 0;
  bool _roomActivity = false;
  VoidCallback? _unbind;

  @override
  void initState() {
    super.initState();
    _unbind = realtime.bind({AtgScope.rooms}, () {
      if (!mounted || _index == 3 || _roomActivity) return;
      setState(() => _roomActivity = true);
    });
  }

  @override
  void dispose() {
    _unbind?.call();
    super.dispose();
  }

  void _select(int index) {
    if (index == _index) return;
    setState(() {
      _index = index;
      if (index == 3) _roomActivity = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final pages = <Widget>[
      HomeScreen(onOpenTab: _select),
      const SignalsScreen(),
      const MarketsScreen(),
      const RoomsScreen(),
      const WalletScreen(),
    ];

    return Scaffold(
      body: IndexedStack(index: _index, children: pages),
      floatingActionButton: AnimatedSwitcher(
        duration: const Duration(milliseconds: 180),
        transitionBuilder: (child, animation) => ScaleTransition(
          scale: animation,
          child: FadeTransition(opacity: animation, child: child),
        ),
        child: _index == 3
            ? const SizedBox.shrink()
            : FloatingActionButton.small(
                key: const ValueKey('atg-chat'),
                tooltip: 'Open the community',
                backgroundColor: AtgColors.primary,
                foregroundColor: AtgColors.ink,
                elevation: 2,
                shape: const CircleBorder(),
                onPressed: () => _select(3),
                child: const Icon(Icons.forum_rounded, size: 20),
              ),
      ),
      bottomNavigationBar: _BottomBar(
        index: _index,
        roomActivity: _roomActivity,
        onSelected: _select,
      ),
    );
  }
}

class _BottomBar extends StatelessWidget {
  const _BottomBar({
    required this.index,
    required this.roomActivity,
    required this.onSelected,
  });

  final int index;
  final bool roomActivity;
  final ValueChanged<int> onSelected;

  static const _icons = <IconData>[
    Icons.home_outlined,
    Icons.bolt_outlined,
    Icons.show_chart_rounded,
    Icons.forum_outlined,
    Icons.account_balance_wallet_outlined,
  ];

  static const _selectedIcons = <IconData>[
    Icons.home_rounded,
    Icons.bolt_rounded,
    Icons.insert_chart_rounded,
    Icons.forum_rounded,
    Icons.account_balance_wallet_rounded,
  ];

  static const _labels = ['Home', 'Signals', 'Markets', 'Community', 'Wallet'];

  @override
  Widget build(BuildContext context) {
    if (Theme.of(context).platform == TargetPlatform.iOS) {
      return Container(
        decoration: const BoxDecoration(
          color: AtgColors.surface,
          border: Border(top: BorderSide(color: AtgColors.stroke)),
        ),
        child: CupertinoTabBar(
          currentIndex: index,
          onTap: onSelected,
          backgroundColor: AtgColors.surface,
          activeColor: AtgColors.gold,
          inactiveColor: AtgColors.muted,
          border: null,
          height: 54,
          items: [
            for (var i = 0; i < _icons.length; i++)
              BottomNavigationBarItem(
                icon: Badge(
                  isLabelVisible: i == 3 && roomActivity,
                  backgroundColor: AtgColors.primary,
                  child: Icon(i == index ? _selectedIcons[i] : _icons[i]),
                ),
                label: _labels[i],
              ),
          ],
        ),
      );
    }

    return Container(
      decoration: const BoxDecoration(
        color: AtgColors.surface,
        border: Border(top: BorderSide(color: AtgColors.stroke)),
      ),
      child: NavigationBar(
        selectedIndex: index,
        onDestinationSelected: onSelected,
        backgroundColor: AtgColors.surface,
        surfaceTintColor: Colors.transparent,
        indicatorColor: AtgColors.primarySoft,
        elevation: 0,
        height: 68,
        labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
        destinations: [
          for (var i = 0; i < _icons.length; i++)
            NavigationDestination(
              icon: Badge(
                isLabelVisible: i == 3 && roomActivity,
                backgroundColor: AtgColors.primary,
                child: Icon(i == index ? _selectedIcons[i] : _icons[i]),
              ),
              label: _labels[i],
            ),
        ],
      ),
    );
  }
}
