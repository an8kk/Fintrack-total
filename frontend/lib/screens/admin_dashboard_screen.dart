import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/user_model.dart';
import '../providers/auth_provider.dart';
import '../services/api_service.dart';
import '../l10n/app_localizations.dart';
import 'package:intl/intl.dart';

class AdminDashboardScreen extends StatefulWidget {
  const AdminDashboardScreen({Key? key}) : super(key: key);

  @override
  State<AdminDashboardScreen> createState() => _AdminDashboardScreenState();
}

class _AdminDashboardScreenState extends State<AdminDashboardScreen> {
  final ApiService _apiService = ApiService();
  List<UserModel> _users = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _fetchUsers();
  }

  Future<void> _fetchUsers() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    try {
      final users = await _apiService.getAllUsers(authProvider.token!);
      setState(() {
        _users = users;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _isLoading = false;
      });
    }
  }

  Future<void> _toggleUserStatus(UserModel user) async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final l10n = AppLocalizations.of(context)!;
    try {
      await _apiService.toggleUserStatus(user.id, authProvider.token!);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.translate('user_status_updated').replaceAll('{name}', user.username))),
      );
      _fetchUsers(); // Refresh list
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error: $e')),
      );
    }
  }

  void _showEditUserDialog(UserModel user) {
    final nameController = TextEditingController(text: user.username);
    final emailController = TextEditingController(text: user.email);

    final l10n = AppLocalizations.of(context)!;
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.translate('edit_user_title').replaceAll('{name}', user.username)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: nameController,
              decoration: InputDecoration(labelText: l10n.translate('username')),
            ),
            TextField(
              controller: emailController,
              decoration: InputDecoration(labelText: l10n.translate('email')),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: Text(l10n.translate('cancel')),
          ),
          ElevatedButton(
            onPressed: () async {
              final authProvider =
                  Provider.of<AuthProvider>(context, listen: false);
              try {
                await _apiService.updateUserByAdmin(
                  user.id,
                  authProvider.token!,
                  nameController.text,
                  emailController.text,
                );
                if (mounted) {
                  Navigator.pop(ctx);
                  _fetchUsers();
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text(l10n.translate('user_updated_success'))),
                  );
                }
              } catch (e) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('Error: $e')),
                );
              }
            },
            child: Text(l10n.translate('save')),
          ),
        ],
      ),
    );
  }

  void _showUserTransactionsDialog(UserModel user) async {
    final l10n = AppLocalizations.of(context)!;
    
    // Show loading dialog first
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => const Center(child: CircularProgressIndicator()),
    );

    try {
      final authProvider = Provider.of<AuthProvider>(context, listen: false);
      final transactions =
          await _apiService.getTransactions(user.id, authProvider.token!);
      
      if (mounted) {
        Navigator.pop(context); // Close loading
        showDialog(
          context: context,
          builder: (ctx) => AlertDialog(
            title: Text(l10n.translate('transactions_title').replaceAll('{name}', user.username)),
            content: SizedBox(
              width: double.maxFinite,
              height: 400,
              child: transactions.isEmpty
                  ? Center(child: Text(l10n.translate('no_transactions_found')))
                  : ListView.builder(
                      itemCount: transactions.length,
                      itemBuilder: (context, index) {
                        final txn = transactions[index];
                        final isExpense = txn.type == 'EXPENSE';
                        return ListTile(
                          leading: CircleAvatar(
                            backgroundColor: isExpense
                                ? Colors.red.withOpacity(0.1)
                                : Colors.green.withOpacity(0.1),
                            child: Icon(
                              isExpense ? Icons.arrow_downward : Icons.arrow_upward,
                              color: isExpense ? Colors.red : Colors.green,
                              size: 16,
                            ),
                          ),
                          title: Text(txn.category),
                          subtitle: Text(DateFormat('dd MMM yyyy').format(txn.date)),
                          trailing: Text(
                            '${isExpense ? "-" : "+"}\$${txn.amount.toStringAsFixed(2)}',
                            style: TextStyle(
                              color: isExpense ? Colors.red : Colors.green,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        );
                      },
                    ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(ctx),
                child: Text(l10n.translate('close')),
              ),
            ],
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        Navigator.pop(context); // Close loading
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.translate('load_transactions_failed').replaceAll('{error}', e.toString()))),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      appBar: AppBar(title: Text(l10n.translate('admin'))),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text('Error: $_error'))
              : ListView.builder(
                  itemCount: _users.length,
                  itemBuilder: (context, index) {
                    final user = _users[index];
                    return Card(
                      margin: const EdgeInsets.symmetric(
                          horizontal: 16, vertical: 8),
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor:
                              user.isBlocked ? Colors.red : Colors.green,
                          child: Icon(
                            user.isBlocked ? Icons.block : Icons.check,
                            color: Colors.white,
                          ),
                        ),
                        title: Text(user.username),
                        subtitle: Text(
                            '${user.email} • ${user.role ?? "USER"}\n${l10n.translate('balance_label')}${user.balance.toStringAsFixed(2)}'),
                        isThreeLine: true,
                        trailing: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            IconButton(
                              icon: const Icon(Icons.receipt_long,
                                  color: Colors.orange),
                              tooltip: l10n.translate('view_transactions_tip'),
                              onPressed: () => _showUserTransactionsDialog(user),
                            ),
                            IconButton(
                              icon: const Icon(Icons.edit, color: Colors.blue),
                              tooltip: l10n.translate('edit_user_tip'),
                              onPressed: () => _showEditUserDialog(user),
                            ),
                            if (user.role != "ADMIN")
                              IconButton(
                                icon: Icon(
                                  user.isBlocked
                                      ? Icons.lock_open
                                      : Icons.lock_outline,
                                  color:
                                      user.isBlocked ? Colors.green : Colors.red,
                                ),
                                tooltip: user.isBlocked ? l10n.translate('unban_tip') : l10n.translate('ban_tip'),
                                onPressed: () => _toggleUserStatus(user),
                              ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
    );
  }
}
