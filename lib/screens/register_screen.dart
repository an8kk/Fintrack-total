import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../l10n/app_localizations.dart';
import '../utils/constants.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _nameController = TextEditingController();
  final _emailController = TextEditingController();
  final _passController = TextEditingController();
  final _confirmPassController = TextEditingController();
  final _saltEdgeController = TextEditingController();

  Future<void> _handleRegister() async {
    final l10n = AppLocalizations.of(context)!;
    if (_passController.text != _confirmPassController.text) {
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(l10n.translate('passwords_not_match'))));
      return;
    }

    final provider = Provider.of<AuthProvider>(context, listen: false);
    try {
      await provider.register(
          _nameController.text, _emailController.text, _passController.text,
          saltEdgeCustomerId: _saltEdgeController.text.isNotEmpty ? _saltEdgeController.text : null);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(l10n.translate('account_created'))));
        Navigator.pop(context);
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text("Error: $e")));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final isLoading = Provider.of<AuthProvider>(context).isLoading;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: AppBar(
        title: Text(l10n.translate('register')),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        elevation: 0,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const SizedBox(height: 20),
            _buildTextField(
                l10n.translate('username'), _nameController, Icons.person_outline, isDark),
            const SizedBox(height: 16),
            _buildTextField(
                l10n.translate('email'), _emailController, Icons.email_outlined, isDark),
            const SizedBox(height: 16),
            _buildTextField(
                l10n.translate('password'), _passController, Icons.lock_outline, isDark,
                isObscure: true),
            const SizedBox(height: 16),
            _buildTextField(l10n.translate('confirm_password'), _confirmPassController,
                Icons.lock_reset_outlined, isDark,
                isObscure: true),
            const SizedBox(height: 16),
            _buildTextField("Salt Edge Customer ID (Optional)", _saltEdgeController,
                Icons.code, isDark),
            const SizedBox(height: 40),
            ElevatedButton(
              onPressed: isLoading ? null : _handleRegister,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 18),
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16)),
                elevation: 0,
              ),
              child: isLoading
                  ? const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(
                          color: Colors.white, strokeWidth: 2))
                  : Text(l10n.translate('register'),
                      style:
                          const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTextField(
      String hint, TextEditingController controller, IconData icon, bool isDark,
      {bool isObscure = false}) {
    final l10n = AppLocalizations.of(context)!;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(hint,
            style: const TextStyle(
                color: Colors.grey, fontSize: 13, fontWeight: FontWeight.w500)),
        const SizedBox(height: 8),
        TextField(
          controller: controller,
          obscureText: isObscure,
          style: TextStyle(color: isDark ? Colors.white : Colors.black),
          decoration: InputDecoration(
            filled: true,
            fillColor: isDark ? Colors.white10 : Colors.grey[100],
            hintText: '${l10n.translate('enter')} $hint',
            prefixIcon: Icon(icon, size: 20),
            border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(16),
                borderSide: BorderSide.none),
          ),
        ),
      ],
    );
  }
}
