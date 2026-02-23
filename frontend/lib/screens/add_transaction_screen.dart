import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/transaction_model.dart';
import '../providers/transaction_provider.dart';
import '../providers/category_provider.dart';
import '../l10n/app_localizations.dart';
import '../utils/constants.dart';

class AddTransactionScreen extends StatefulWidget {
  const AddTransactionScreen({super.key});

  @override
  State<AddTransactionScreen> createState() => _AddTransactionScreenState();
}

class _AddTransactionScreenState extends State<AddTransactionScreen> {
  final _amountController = TextEditingController();
  final _descController = TextEditingController();
  String _selectedType = 'EXPENSE';
  String? _selectedCategory;

  @override
  void initState() {
    super.initState();
    Future.microtask(() =>
        Provider.of<CategoryProvider>(context, listen: false)
            .fetchCategories());
  }

  void _submit() async {
    if (_amountController.text.isEmpty || _selectedCategory == null) return;

    final tx = TransactionModel(
      amount: double.parse(_amountController.text),
      category: _selectedCategory!,
      description: _descController.text,
      date: DateTime.now(),
      type: _selectedType,
    );

    try {
      await Provider.of<TransactionProvider>(context, listen: false)
          .addTransaction(tx);
      if (mounted) Navigator.pop(context);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text("Error: $e")));
      }
    }
  }

  @override
  Widget build(BuildContext context) {

    final l10n = AppLocalizations.of(context)!;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
          title: Text(l10n.translate('add_transaction')),
          backgroundColor: AppColors.primary,
          foregroundColor: Colors.white),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Expanded(child: _buildTypeButton('EXPENSE', l10n.translate('expense'))),
                const SizedBox(width: 16),
                Expanded(child: _buildTypeButton('INCOME', l10n.translate('income'))),
              ],
            ),
            const SizedBox(height: 24),
            Text(l10n.translate('amount'), style: const TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            TextField(
              controller: _amountController,
              keyboardType: TextInputType.number,
              style: TextStyle(color: isDark ? Colors.white : Colors.black),
              decoration: InputDecoration(
                hintText: "0.00",
                filled: true,
                fillColor: isDark ? Colors.white10 : Colors.grey[100],
                border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide.none),
              ),
            ),
            const SizedBox(height: 16),
            Text(l10n.translate('category'),
                style: const TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Consumer<CategoryProvider>(
              builder: (context, provider, child) {
                if (provider.categories.isEmpty) {
                  return const LinearProgressIndicator();
                }


                final filteredCategories = provider.categories
                    .where((c) => c.type == _selectedType)
                    .toList();


                if (_selectedCategory != null &&
                    !filteredCategories
                        .any((c) => c.name == _selectedCategory)) {
                  _selectedCategory = null;
                }

                return DropdownButtonFormField<String>(
                  initialValue: _selectedCategory,
                  dropdownColor:
                      isDark ? const Color(0xFF1E1E1E) : Colors.white,
                  hint: Text(l10n.translate('select_category')),
                  onChanged: (val) => setState(() => _selectedCategory = val),
                  items: filteredCategories.map<DropdownMenuItem<String>>((c) {
                    return DropdownMenuItem<String>(
                      value: c.name,
                      child: Text(c.name,
                          style: TextStyle(
                              color: isDark ? Colors.white : Colors.black)),
                    );
                  }).toList(),
                  decoration: InputDecoration(
                    filled: true,
                    fillColor: isDark ? Colors.white10 : Colors.grey[100],
                    border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                        borderSide: BorderSide.none),
                  ),
                );
              },
            ),
            const SizedBox(height: 16),
            Text(l10n.translate('description'),
                style: const TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            TextField(
              controller: _descController,
              style: TextStyle(color: isDark ? Colors.white : Colors.black),
              decoration: InputDecoration(
                hintText: l10n.translate('description_hint'),
                filled: true,
                fillColor: isDark ? Colors.white10 : Colors.grey[100],
                border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide.none),
              ),
            ),
            const Spacer(),
            ElevatedButton(
              onPressed: _submit,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12)),
              ),
              child: Text(l10n.translate('add_transaction')),
            )
          ],
        ),
      ),
    );
  }

  Widget _buildTypeButton(String type, String label) {
    final isSelected = _selectedType == type;
    return GestureDetector(
      onTap: () => setState(() => _selectedType = type),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 12),
        decoration: BoxDecoration(
          color: isSelected
              ? (type == 'EXPENSE' ? Colors.red : AppColors.primary)
              : Colors.grey.withValues(alpha: 0.2),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Center(
          child: Text(
            label,
            style: TextStyle(
                color: isSelected ? Colors.white : Colors.grey,
                fontWeight: FontWeight.bold),
          ),
        ),
      ),
    );
  }
}
