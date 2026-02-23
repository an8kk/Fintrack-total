import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../providers/transaction_provider.dart';
import '../l10n/app_localizations.dart';
import '../utils/constants.dart';

class AllTransactionsScreen extends StatelessWidget {
  const AllTransactionsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: Text(l10n.translate('transaction_history')),
          backgroundColor: AppColors.primary,
          foregroundColor: Colors.white,
          bottom: TabBar(
            labelColor: Colors.white,
            unselectedLabelColor: Colors.white70,
            indicatorColor: Colors.white,
            tabs: [Tab(text: l10n.translate('expenses')), Tab(text: l10n.translate('income'))],
          ),
        ),
        body: const TabBarView(
          children: [
            TransactionList(type: "EXPENSE"),
            TransactionList(type: "INCOME"),
          ],
        ),
      ),
    );
  }
}

class TransactionList extends StatelessWidget {
  final String type;
  const TransactionList({super.key, required this.type});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final provider = Provider.of<TransactionProvider>(context);
    final filtered =
        provider.transactions.where((t) => t.type == type).toList();
    final currency = NumberFormat.simpleCurrency(locale: 'en_US');
    final isDark = Theme.of(context).brightness == Brightness.dark;

    if (filtered.isEmpty) {
      return Center(
          child: Text(l10n.translate('no_data'), style: const TextStyle(color: Colors.grey)));
    }

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: filtered.length,
      itemBuilder: (ctx, i) {
        final tx = filtered[i];
        return Card(
          elevation: 0,
          color: isDark ? const Color(0xFF1E1E1E) : Colors.white,
          margin: const EdgeInsets.only(bottom: 12),
          shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
              side: BorderSide(
                  color: isDark ? Colors.white10 : Colors.grey.shade100)),
          child: ListTile(
            leading: CircleAvatar(
              backgroundColor:
                  tx.type == 'EXPENSE' ? AppColors.expense : AppColors.income,
              child: Icon(
                tx.type == 'EXPENSE'
                    ? Icons.shopping_bag_outlined
                    : Icons.attach_money,
                color: tx.type == 'EXPENSE'
                    ? AppColors.expenseText
                    : AppColors.incomeText,
              ),
            ),
            title: Text(tx.category,
                style: const TextStyle(fontWeight: FontWeight.bold)),
            subtitle: Text(DateFormat('dd MMMM, yyyy').format(tx.date),
                style: const TextStyle(fontSize: 12)),
            trailing: Text(
              "${tx.type == 'EXPENSE' ? '-' : '+'}${currency.format(tx.amount)}",
              style: TextStyle(
                color: tx.type == 'EXPENSE' ? Colors.red : Colors.green,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        );
      },
    );
  }
}
