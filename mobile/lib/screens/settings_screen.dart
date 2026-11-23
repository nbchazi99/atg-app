import 'package:flutter/material.dart';

import '../app_state.dart';
import '../responsive.dart';
import '../theme.dart';
import '../widgets.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  Future<void> _saveDarkMode(BuildContext context, bool darkMode) async {
    try {
      await session.saveDarkMode(darkMode);
    } on StateError catch (error) {
      if (context.mounted) showToast(context, error.message);
    } on Exception catch (error) {
      if (context.mounted) {
        showToast(context, 'Could not save appearance preference: $error');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      body: SafeArea(
        child: PageBody(
          maxWidth: 760,
          child: Column(
            children: [
              const DetailHeader(title: 'Settings'),
              Expanded(
                child: ListView(
                  padding: const EdgeInsets.fromLTRB(16, 8, 16, 28),
                  children: [
                    const SectionLabel('APPEARANCE'),
                    const SizedBox(height: 8),
                    SurfaceCard(
                      padding: const EdgeInsets.fromLTRB(14, 10, 8, 10),
                      child: Row(
                        children: [
                          Icon(
                            session.isDarkMode
                                ? Icons.dark_mode_outlined
                                : Icons.light_mode_outlined,
                            color: AtgColors.gold,
                            size: 20,
                          ),
                          const SizedBox(width: 10),
                          const Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  'High-contrast dark mode',
                                  style: AtgText.h3,
                                ),
                                SizedBox(height: 2),
                                Text(
                                  'Use a darker theme throughout ATG.',
                                  style: AtgText.small,
                                ),
                              ],
                            ),
                          ),
                          Switch.adaptive(
                            value: session.isDarkMode,
                            onChanged: (value) => _saveDarkMode(context, value),
                            activeTrackColor: AtgColors.primary,
                          ),
                        ],
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
