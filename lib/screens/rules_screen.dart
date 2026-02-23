import 'package:flutter/material.dart';
import '../l10n/app_localizations.dart';
import '../utils/constants.dart';

class RulesScreen extends StatelessWidget {
  const RulesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      appBar: AppBar(
          title: Text(l10n.translate('terms_of_use')),
          backgroundColor: AppColors.primary,
          foregroundColor: Colors.white),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Text(
          l10n.translate('rules_content'),
          style: const TextStyle(fontSize: 16, height: 1.5),
        ),
      ),
    );
  }
}
