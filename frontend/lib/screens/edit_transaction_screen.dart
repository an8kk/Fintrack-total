import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../models/transaction_model.dart';
import '../models/category_model.dart';
import '../providers/auth_provider.dart';
import '../l10n/app_localizations.dart';
import '../services/api_service.dart';

class EditTransactionScreen extends StatefulWidget {
  final TransactionModel transaction;
  final VoidCallback onTransactionUpdated;

  const EditTransactionScreen({
    Key? key,
    required this.transaction,
    required this.onTransactionUpdated,
  }) : super(key: key);

  @override
  State<EditTransactionScreen> createState() => _EditTransactionScreenState();
}

class _EditTransactionScreenState extends State<EditTransactionScreen> {
  final _formKey = GlobalKey<FormState>();
  late double _amount;
  late String _category;
  late String _description;
  late DateTime _selectedDate;
  late String _type;
  bool _isLoading = false;

  final ApiService _apiService = ApiService();
  List<CategoryModel> _categories = [];

  @override
  void initState() {
    super.initState();
    _amount = widget.transaction.amount;
    _category = widget.transaction.category;
    _description = widget.transaction.description;
    _selectedDate = widget.transaction.date;
    _type = widget.transaction.type;
    _fetchCategories();
  }

  Future<void> _fetchCategories() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    try {
      final categories = await _apiService.getCategories(
          authProvider.currentUserId!, authProvider.token!);
      setState(() {
        _categories = categories;
      });
    } catch (e) {
      // Handle error
    }
  }

  Future<void> _submitForm() async {
    final l10n = AppLocalizations.of(context)!;
    if (!_formKey.currentState!.validate()) return;
    _formKey.currentState!.save();

    setState(() => _isLoading = true);

    final authProvider = Provider.of<AuthProvider>(context, listen: false);

    try {
      final updatedTransaction = TransactionModel(
        id: widget.transaction.id,
        amount: _amount,
        category: _category,
        description: _description,
        date: _selectedDate,
        type: _type,
      );

      await _apiService.updateTransaction(
        authProvider.currentUserId!,
        authProvider.token!,
        widget.transaction.id!,
        updatedTransaction,
      );

      widget.onTransactionUpdated();
      if (mounted) Navigator.pop(context);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.translate('transaction_updated'))),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      appBar: AppBar(title: Text(l10n.translate('edit_transaction'))),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Padding(
              padding: const EdgeInsets.all(16.0),
              child: Form(
                key: _formKey,
                child: ListView(
                  children: [
                    // Type Selector
                    Row(
                      children: [
                        Expanded(
                          child: RadioListTile<String>(
                            title: Text(l10n.translate('expense')),
                            value: 'EXPENSE',
                            groupValue: _type,
                            onChanged: (val) => setState(() => _type = val!),
                          ),
                        ),
                        Expanded(
                          child: RadioListTile<String>(
                            title: Text(l10n.translate('income')),
                            value: 'INCOME',
                            groupValue: _type,
                            onChanged: (val) => setState(() => _type = val!),
                          ),
                        ),
                      ],
                    ),

                    // Amount
                    TextFormField(
                      initialValue: _amount.toString(),
                      decoration: InputDecoration(labelText: l10n.translate('amount')),
                      keyboardType: TextInputType.number,
                      validator: (val) =>
                          val == null || val.isEmpty ? l10n.translate('enter_amount') : null,
                      onSaved: (val) => _amount = double.parse(val!),
                    ),

                    // Category
                    DropdownButtonFormField<String>(
                      value: _categories.any((c) => c.name == _category) ? _category : null,
                      decoration: InputDecoration(labelText: l10n.translate('category')),
                      items: _categories
                          .map((c) => DropdownMenuItem(
                                value: c.name,
                                child: Text(c.name),
                              ))
                          .toList(),
                      onChanged: (val) => setState(() => _category = val!),
                      validator: (val) =>
                          val == null ? l10n.translate('select_category') : null,
                    ),

                    // Description
                    TextFormField(
                      initialValue: _description,
                      decoration: InputDecoration(labelText: l10n.translate('description')),
                      onSaved: (val) => _description = val!,
                    ),

                    // Date Picker
                    ListTile(
                      title: Text(
                          'Date: ${DateFormat('yyyy-MM-dd').format(_selectedDate)}'),
                      trailing: const Icon(Icons.calendar_today),
                      onTap: () async {
                        final picked = await showDatePicker(
                          context: context,
                          initialDate: _selectedDate,
                          firstDate: DateTime(2000),
                          lastDate: DateTime.now(),
                        );
                        if (picked != null) {
                          setState(() => _selectedDate = picked);
                        }
                      },
                    ),

                    const SizedBox(height: 20),
                    ElevatedButton(
                      onPressed: _submitForm,
                      child: Text(l10n.translate('save')),
                    ),
                  ],
                ),
              ),
            ),
    );
  }
}
