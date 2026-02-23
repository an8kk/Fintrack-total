import 'package:flutter/material.dart';
import '../l10n/app_localizations.dart';
import '../utils/constants.dart';

class HelpScreen extends StatelessWidget {
  const HelpScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
          title: Text(l10n.translate('help_faq')),
          backgroundColor: AppColors.primary,
          foregroundColor: Colors.white),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          _buildHelpTile(
              l10n.translate('faq_1_q'),
              l10n.translate('faq_1_a'),
              isDark),
          _buildHelpTile(
              l10n.translate('faq_2_q'),
              l10n.translate('faq_2_a'),
              isDark),
          _buildHelpTile(
              l10n.translate('faq_3_q'),
              l10n.translate('faq_3_a'),
              isDark),
          _buildHelpTile(
              l10n.translate('faq_4_q'),
              l10n.translate('faq_4_a'),
              isDark),
          const SizedBox(height: 30),
          Center(
              child: Text(l10n.translate('app_version'),
                  style: TextStyle(color: Colors.grey[600], fontSize: 12))),
        ],
      ),
    );
  }

  Widget _buildHelpTile(String title, String content, bool isDark) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
          color: isDark ? Colors.white.withOpacity(0.05) : Colors.grey[50],
          borderRadius: BorderRadius.circular(16)),
      child: ExpansionTile(
        shape: const Border(),
        title: Text(title,
            style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15)),
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
            child: Text(content,
                style: TextStyle(
                    color: isDark ? Colors.white70 : Colors.black54,
                    height: 1.5)),
          )
        ],
      ),
    );
  }
}
