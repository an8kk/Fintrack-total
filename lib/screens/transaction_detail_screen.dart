import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../models/transaction_model.dart';
import '../providers/transaction_provider.dart';
import '../providers/auth_provider.dart';
import '../services/api_service.dart';
import '../l10n/app_localizations.dart';
import '../utils/constants.dart';
import 'edit_transaction_screen.dart';

class TransactionDetailScreen extends StatefulWidget {
  final TransactionModel transaction;

  const TransactionDetailScreen({super.key, required this.transaction});

  @override
  State<TransactionDetailScreen> createState() => _TransactionDetailScreenState();
}

class _TransactionDetailScreenState extends State<TransactionDetailScreen> {
  final ApiService _apiService = ApiService();
  String? _aiInsight;
  bool _isLoadingInsight = false;

  Future<void> _fetchAiInsight() async {
    if (widget.transaction.id == null) return;
    final auth = Provider.of<AuthProvider>(context, listen: false);
    if (auth.currentUserId == null || auth.token == null) return;

    setState(() => _isLoadingInsight = true);
    try {
      final insight = await _apiService.getTransactionInsight(
          auth.currentUserId!, widget.transaction.id!, auth.token!);
      if (mounted) setState(() => _aiInsight = insight);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('AI insight failed: $e'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoadingInsight = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final transaction = widget.transaction;
    final isExpense = transaction.type == 'EXPENSE';
    final provider = Provider.of<TransactionProvider>(context, listen: false);
    final isDark = Theme.of(context).brightness == Brightness.dark;

    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: AppBar(
        title: Text(l10n.translate('transaction_details')),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        elevation: 0,
      ),
      body: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          children: [

            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(vertical: 40, horizontal: 20),
              decoration: BoxDecoration(
                color: isDark ? const Color(0xFF1E1E1E) : Colors.white,
                borderRadius: BorderRadius.circular(28),
                boxShadow: [
                  BoxShadow(
                      color: Colors.black.withOpacity(0.03),
                      blurRadius: 20,
                      offset: const Offset(0, 10))
                ],
              ),
              child: Column(
                children: [
                  Text(l10n.translate('transaction_amount_label'),
                      style: TextStyle(color: Colors.grey[500], fontSize: 14)),
                  const SizedBox(height: 12),
                  Text(
                    "${isExpense ? '-' : '+'}\$${transaction.amount.toStringAsFixed(2)}",
                    style: TextStyle(
                        fontSize: 42,
                        fontWeight: FontWeight.bold,
                        color: isExpense ? Colors.redAccent : Colors.green),
                  ),
                  const SizedBox(height: 16),
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    decoration: BoxDecoration(
                      color: isExpense
                          ? Colors.red.withOpacity(0.1)
                          : Colors.green.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      isExpense ? l10n.translate('expense') : l10n.translate('income'),
                      style: TextStyle(
                          fontWeight: FontWeight.bold,
                          color: isExpense ? Colors.red : Colors.green),
                    ),
                  )
                ],
              ),
            ),
            const SizedBox(height: 20),


            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: isDark ? const Color(0xFF1E1E1E) : Colors.white,
                borderRadius: BorderRadius.circular(28),
              ),
              child: Column(
                children: [
                  _buildDetailRow(
                      context,
                      Icons.description_outlined,
                      l10n.translate('description'),
                      transaction.description.isEmpty
                          ? l10n.translate('no_description')
                          : transaction.description,
                      isDark),
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    child: Divider(
                        color: isDark ? Colors.white10 : Colors.grey[100]),
                  ),
                  _buildDetailRow(context, Icons.category_outlined, l10n.translate('category'),
                      transaction.category, isDark),
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    child: Divider(
                        color: isDark ? Colors.white10 : Colors.grey[100]),
                  ),
                  _buildDetailRow(
                      context,
                      Icons.calendar_today_rounded,
                      l10n.translate('date_time'),
                      DateFormat('dd MMMM yyyy, HH:mm')
                          .format(transaction.date),
                      isDark),
                ],
              ),
            ),

            const SizedBox(height: 16),

            // ─── AI Insight Button & Card ─────────────────────────
            SizedBox(
              width: double.infinity,
              child: TextButton.icon(
                onPressed: _isLoadingInsight ? null : _fetchAiInsight,
                icon: _isLoadingInsight
                    ? const SizedBox(
                        width: 18, height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2, color: Colors.deepPurple))
                    : const Icon(Icons.auto_awesome, color: Colors.deepPurple),
                label: Text(
                  _aiInsight == null ? '✨ AI Insight' : '✨ Refresh Insight',
                  style: const TextStyle(
                      color: Colors.deepPurple,
                      fontWeight: FontWeight.bold,
                      fontSize: 15),
                ),
                style: TextButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                      side: const BorderSide(color: Colors.deepPurple, width: 1)),
                ),
              ),
            ),

            if (_aiInsight != null) ...[
              const SizedBox(height: 12),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: isDark
                        ? [const Color(0xFF2D1B69), const Color(0xFF1A1A2E)]
                        : [const Color(0xFFEDE7F6), const Color(0xFFF3E5F5)],
                  ),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Icon(Icons.auto_awesome, color: Colors.deepPurple, size: 20),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        _aiInsight!,
                        style: TextStyle(
                          fontSize: 13,
                          color: isDark ? Colors.white70 : Colors.black87,
                          height: 1.4,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],

            const Spacer(),

            // 3. Edit Action
            SizedBox(
              width: double.infinity,
              child: TextButton.icon(
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => EditTransactionScreen(
                        transaction: transaction,
                        onTransactionUpdated: () {
                           Navigator.pop(context);
                           Provider.of<TransactionProvider>(context, listen: false).fetchTransactions();
                        },
                      ),
                    ),
                  );
                },
                icon: const Icon(Icons.edit, color: Colors.blue),
                label: Text(l10n.translate('edit'),
                    style: const TextStyle(
                        color: Colors.blue,
                        fontWeight: FontWeight.bold,
                        fontSize: 16)),
                style: TextButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 18),
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                      side:
                          const BorderSide(color: Colors.blue, width: 1)),
                ),
              ),
            ),
            const SizedBox(height: 10),


            SizedBox(
              width: double.infinity,
              child: TextButton.icon(
                onPressed: () => _confirmDeletion(context, provider),
                icon: const Icon(Icons.delete_forever_rounded,
                    color: Colors.redAccent),
                label: Text(l10n.translate('delete_entry'),
                    style: const TextStyle(
                        color: Colors.redAccent,
                        fontWeight: FontWeight.bold,
                        fontSize: 16)),
                style: TextButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 18),
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                      side:
                          const BorderSide(color: Colors.redAccent, width: 1)),
                ),
              ),
            ),
            const SizedBox(height: 10),
          ],
        ),
      ),
    );
  }

  void _confirmDeletion(BuildContext context, TransactionProvider provider) {
    final l10n = AppLocalizations.of(context)!;
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.translate('delete_transaction_confirm_title')),
        content: Text(l10n.translate('delete_transaction_confirm_msg')),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx), child: Text(l10n.translate('cancel'))),
          TextButton(
              onPressed: () async {
                await provider.deleteTransaction(widget.transaction.id!);
                if (context.mounted) {
                  Navigator.pop(ctx);
                  Navigator.pop(context);
                }
              },
              child:
                  Text(l10n.translate('delete'), style: const TextStyle(color: Colors.red))),
        ],
      ),
    );
  }

  Widget _buildDetailRow(BuildContext context, IconData icon, String label,
      String value, bool isDark) {
    return Row(
      children: [
        Container(
          padding: const EdgeInsets.all(10),
          decoration: BoxDecoration(
            color: isDark ? Colors.white : Colors.grey[50],
            borderRadius: BorderRadius.circular(12),
          ),
          child: Icon(icon, color: Colors.grey[600], size: 22),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label,
                  style: const TextStyle(
                      color: Colors.grey,
                      fontSize: 12,
                      fontWeight: FontWeight.w500)),
              const SizedBox(height: 2),
              Text(
                value,
                style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 15,
                    color: isDark ? Colors.white : Colors.black87),
              ),
            ],
          ),
        )
      ],
    );
  }
}
