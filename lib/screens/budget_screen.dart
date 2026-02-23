import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/transaction_provider.dart';
import '../providers/category_provider.dart';
import '../l10n/app_localizations.dart';
import '../utils/constants.dart';

class BudgetScreen extends StatelessWidget {
  const BudgetScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final provider = Provider.of<TransactionProvider>(context);
    final catProvider = Provider.of<CategoryProvider>(context);
    final isDark = Theme.of(context).brightness == Brightness.dark;

    double totalBudget = 0;
    double totalSpent = 0;
    Map<String, double> spentByCategory = {};

    for (var t in provider.transactions) {
      if (t.type == 'EXPENSE') {
        spentByCategory[t.category] =
            (spentByCategory[t.category] ?? 0) + t.amount;
        totalSpent += t.amount;
      }
    }

    for (var c in catProvider.categories) {
      if (c.type == 'EXPENSE') totalBudget += c.budgetLimit;
    }

    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      appBar: AppBar(
          title: Text(l10n.translate('budget')),
          backgroundColor: AppColors.primary,
          foregroundColor: Colors.white),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                  color: isDark ? const Color(0xFF1E1E1E) : Colors.white,
                  borderRadius: BorderRadius.circular(20)),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(l10n.translate('total_limit'),
                              style: const TextStyle(color: Colors.grey)),
                          Text("\$${totalBudget.toStringAsFixed(0)}",
                              style: const TextStyle(
                                  fontSize: 24, fontWeight: FontWeight.bold)),
                        ],
                      ),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          Text(l10n.translate('spent'),
                              style: const TextStyle(color: Colors.grey)),
                          Text("\$${totalSpent.toStringAsFixed(0)}",
                              style: const TextStyle(
                                  fontSize: 24,
                                  fontWeight: FontWeight.bold,
                                  color: Colors.red)),
                        ],
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  LinearProgressIndicator(
                    value: totalBudget == 0
                        ? 0
                        : (totalSpent / totalBudget).clamp(0.0, 1.0),
                    backgroundColor: isDark ? Colors.white10 : Colors.grey[200],
                    color: AppColors.primary,
                    minHeight: 10,
                    borderRadius: BorderRadius.circular(10),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            Align(
                alignment: Alignment.centerLeft,
                child: Text(l10n.translate('details'),
                    style:
                        const TextStyle(fontSize: 18, fontWeight: FontWeight.bold))),
            const SizedBox(height: 12),
            ...catProvider.categories
                .where((c) => c.type == 'EXPENSE')
                .map((cat) {
              double limit = cat.budgetLimit;
              double spent = spentByCategory[cat.name] ?? 0;
              double percent = limit == 0 ? 0 : (spent / limit).clamp(0.0, 1.0);
              Color catColor = Color(int.parse(
                  cat.color.replaceAll('#', '0xFF')));

              return Container(
                margin: const EdgeInsets.only(bottom: 12),
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                    color: isDark ? const Color(0xFF1E1E1E) : Colors.white,
                    borderRadius: BorderRadius.circular(16)),
                child: Column(
                  children: [
                    Row(
                      children: [
                        CircleAvatar(radius: 6, backgroundColor: catColor),
                        const SizedBox(width: 8),
                        Text(cat.name,
                            style:
                                const TextStyle(fontWeight: FontWeight.bold)),
                        const Spacer(),
                        Text("\$${spent.toStringAsFixed(0)} / \$$limit"),
                      ],
                    ),
                    const SizedBox(height: 12),
                    LinearProgressIndicator(
                      value: percent,
                      backgroundColor:
                          isDark ? Colors.white10 : Colors.grey[100],
                      color: catColor,
                      minHeight: 6,
                      borderRadius: BorderRadius.circular(10),
                    ),
                  ],
                ),
              );
            }).toList()
          ],
        ),
      ),
    );
  }
}
