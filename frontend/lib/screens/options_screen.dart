import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../providers/auth_provider.dart';
import '../providers/theme_provider.dart';
import '../providers/language_provider.dart';
import '../l10n/app_localizations.dart';
import '../utils/constants.dart';
import 'login_screen.dart';
import 'help_screen.dart';
import 'rules_screen.dart';
import 'reports_screen.dart';
import 'salt_edge_screen.dart';

class OptionsScreen extends StatefulWidget {
  const OptionsScreen({super.key});

  @override
  State<OptionsScreen> createState() => _OptionsScreenState();
}

class _OptionsScreenState extends State<OptionsScreen> {
  bool _notifEnabled = false;

  @override
  void initState() {
    super.initState();
    _loadPrefs();
  }

  Future<void> _loadPrefs() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _notifEnabled = prefs.getBool('notifications_enabled') ?? true;
    });
  }

  Future<void> _toggleNotif(bool value) async {
    final l10n = AppLocalizations.of(context)!;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('notifications_enabled', value);
    setState(() => _notifEnabled = value);
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(value ? l10n.translate('notif_on') : l10n.translate('notif_off')),
      duration: const Duration(seconds: 1),
    ));
  }

  void _showLanguageDialog() {
    final lp = Provider.of<LanguageProvider>(context, listen: false);
    final l10n = AppLocalizations.of(context)!;

    final languages = [
      {'name': 'English', 'locale': const Locale('en')},
      {'name': 'Русский', 'locale': const Locale('ru')},
      {'name': 'Español', 'locale': const Locale('es')},
      {'name': 'Français', 'locale': const Locale('fr')},
      {'name': 'Deutsch', 'locale': const Locale('de')},
    ];

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.translate('language')),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: languages
              .map((lang) => ListTile(
                    title: Text(lang['name'] as String),
                    trailing: lp.locale.languageCode ==
                            (lang['locale'] as Locale).languageCode
                        ? const Icon(Icons.check, color: AppColors.primary)
                        : null,
                    onTap: () {
                      lp.setLocale(lang['locale'] as Locale);
                      Navigator.pop(ctx);
                    },
                  ))
              .toList(),
        ),
      ),
    );
  }


  void _showEditDialog(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final provider = Provider.of<AuthProvider>(context, listen: false);
    final nameController = TextEditingController(text: provider.username);
    final emailController = TextEditingController(text: provider.email ?? "");

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.translate('edit_profile')),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
                controller: nameController,
                decoration: InputDecoration(labelText: l10n.translate('username'))),
            const SizedBox(height: 10),
            TextField(
                controller: emailController,
                decoration: InputDecoration(labelText: l10n.translate('email'))),
          ],
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx), child: Text(l10n.translate('cancel'))),
          ElevatedButton(
            onPressed: () async {
              try {
                await provider.updateProfile(
                    nameController.text, emailController.text);
                if (context.mounted) Navigator.pop(ctx);
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text(l10n.translate('updated_successfully'))));
                }
              } catch (e) {/* Error */}
            },
            style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: Colors.white),
            child: Text(l10n.translate('save')),
          ),
        ],
      ),
    );
  }

  void _showChangePasswordDialog(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final oldPassController = TextEditingController();
    final newPassController = TextEditingController();
    final provider = Provider.of<AuthProvider>(context, listen: false);

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.translate('change_password')),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
                controller: oldPassController,
                obscureText: true,
                decoration: InputDecoration(labelText: l10n.translate('old_password'))),
            const SizedBox(height: 10),
            TextField(
                controller: newPassController,
                obscureText: true,
                decoration: InputDecoration(labelText: l10n.translate('new_password'))),
          ],
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx), child: Text(l10n.translate('cancel'))),
          ElevatedButton(
            onPressed: () async {
              try {
                await provider.changePassword(
                    oldPassController.text, newPassController.text);
                if (context.mounted) Navigator.pop(ctx);
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text(l10n.translate('password_changed'))));
                }
              } catch (e) {
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                    content: Text("Error: $e"), backgroundColor: Colors.red));
                }
              }
            },
            style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: Colors.white),
            child: Text(l10n.translate('change')),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final provider = Provider.of<AuthProvider>(context);
    final themeProvider = Provider.of<ThemeProvider>(context);
    final isDark = themeProvider.isDarkMode;

    final l10n = AppLocalizations.of(context)!;
    final lp = Provider.of<LanguageProvider>(context);

    String currentLangName = "English";
    if (lp.locale.languageCode == 'ru') currentLangName = "Русский";
    if (lp.locale.languageCode == 'es') currentLangName = "Español";
    if (lp.locale.languageCode == 'fr') currentLangName = "Français";
    if (lp.locale.languageCode == 'de') currentLangName = "Deutsch";

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: AppBar(
        title: Text(l10n.translate('settings')),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        elevation: 0,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildSectionHeader(l10n.translate('profile')),
            Container(
              decoration: _cardDecoration(context),
              child: Column(
                children: [
                  _buildTile(context, Icons.person_outline, l10n.translate('edit'),
                      onTap: () => _showEditDialog(context)),
                  Divider(height: 1, indent: 50, color: Colors.grey[300]),
                  _buildTile(context, Icons.lock_outline, l10n.translate('change_password'),
                      onTap: () => _showChangePasswordDialog(context)),
                ],
              ),
            ),
            const SizedBox(height: 20),
            const SizedBox(height: 20),
            _buildSectionHeader(l10n.translate('preferences')),
            Container(
              decoration: _cardDecoration(context),
              child: Column(
                children: [
                  _buildSwitchTile(context, Icons.notifications_none,
                      l10n.translate('notifications'), _notifEnabled, _toggleNotif),
                  Divider(height: 1, indent: 50, color: Colors.grey[300]),
                  _buildSwitchTile(
                      context,
                      Icons.dark_mode_outlined,
                      l10n.translate('dark_mode'),
                      isDark,
                      (val) => themeProvider.toggleTheme(val)),
                  Divider(height: 1, indent: 50, color: Colors.grey[300]),
                  _buildTile(context, Icons.language, l10n.translate('language'),
                      trailingText: currentLangName, onTap: _showLanguageDialog),
                  Divider(height: 1, indent: 50, color: Colors.grey[300]),
                  _buildTile(context, Icons.assessment_outlined, l10n.translate('financial_reports'),
                      onTap: () => Navigator.push(
                          context,
                          MaterialPageRoute(
                              builder: (_) => const ReportsScreen()))),
                  Divider(height: 1, indent: 50, color: Colors.grey[300]),
                  _buildTile(context, Icons.account_balance_outlined, l10n.translate('bank_connection'),
                      onTap: () => Navigator.push(
                          context,
                          MaterialPageRoute(
                              builder: (_) => const SaltEdgeScreen()))),
                ],
              ),
            ),
            const SizedBox(height: 20),
            const SizedBox(height: 20),
            _buildSectionHeader(l10n.translate('support')),
            Container(
              decoration: _cardDecoration(context),
              child: Column(
                children: [
                  _buildTile(context, Icons.help_outline, l10n.translate('help'),
                      onTap: () => Navigator.push(
                          context,
                          MaterialPageRoute(
                              builder: (_) => const HelpScreen()))),
                  Divider(height: 1, indent: 50, color: Colors.grey[300]),
                  _buildTile(context, Icons.description_outlined,
                      l10n.translate('terms_of_service'),
                      onTap: () => Navigator.push(
                          context,
                          MaterialPageRoute(
                              builder: (_) => const RulesScreen()))),
                ],
              ),
            ),
            const SizedBox(height: 30),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: () {
                  provider.logout();
                  Navigator.pushAndRemoveUntil(
                      context,
                      MaterialPageRoute(builder: (_) => const LoginScreen()),
                      (r) => false);
                },
                icon: const Icon(Icons.logout, color: Colors.red),
                label: Text(l10n.translate('logout'),
                    style: const TextStyle(
                        color: Colors.red, fontWeight: FontWeight.bold)),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Theme.of(context).cardColor,
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12)),
                  elevation: 0,
                ),
              ),
            ),
            const SizedBox(height: 20),
            const Center(
                child: Text("FinTrack v1.0.0",
                    style: TextStyle(color: Colors.grey))),
          ],
        ),
      ),
    );
  }

  BoxDecoration _cardDecoration(BuildContext context) {
    return BoxDecoration(
        color: Theme.of(context).cardColor,
        borderRadius: BorderRadius.circular(16));
  }

  Widget _buildSectionHeader(String title) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8, left: 4),
      child:
          Text(title, style: const TextStyle(color: Colors.grey, fontSize: 13)),
    );
  }

  Widget _buildTile(BuildContext context, IconData icon, String title,
      {VoidCallback? onTap, String? trailingText}) {
    final color = Theme.of(context).iconTheme.color ?? Colors.black87;
    return ListTile(
      leading: Icon(icon, color: color),
      title: Text(title,
          style: TextStyle(
              fontSize: 15,
              color: Theme.of(context).textTheme.bodyLarge?.color)),
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (trailingText != null)
            Text(trailingText,
                style: const TextStyle(color: Colors.grey, fontSize: 13)),
          const SizedBox(width: 8),
          const Icon(Icons.arrow_forward_ios, size: 14, color: Colors.grey),
        ],
      ),
      onTap: onTap,
    );
  }

  Widget _buildSwitchTile(BuildContext context, IconData icon, String title,
      bool value, Function(bool) onChanged) {
    final color = Theme.of(context).iconTheme.color ?? Colors.black87;
    return SwitchListTile(
      secondary: Icon(icon, color: color),
      title: Text(title,
          style: TextStyle(
              fontSize: 15,
              color: Theme.of(context).textTheme.bodyLarge?.color)),
      value: value,
      onChanged: onChanged,
      activeColor: AppColors.primary,
    );
  }
}
