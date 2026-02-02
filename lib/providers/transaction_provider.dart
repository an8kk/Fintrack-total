import 'package:flutter/material.dart';
import '../models/transaction_model.dart';
import 'package:frontend/services/api_service.dart';

class TransactionProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();

  List<TransactionModel> _transactions = [];
  bool _isLoading = false;
  String? _error;

  // Hardcoded User ID for now (Since backend auth isn't built yet)
  final int currentUserId = 1;

  List<TransactionModel> get transactions => _transactions;
  bool get isLoading => _isLoading;
  String? get error => _error;

  double get totalBalance {
    double balance =
        913488; // In a real app, fetch initial balance from User endpoint
    // We assume initial balance is 0 + income - expense for this demo
    // Or you can hardcode a starting balance if the backend User has one.
    for (var t in _transactions) {
      if (t.type == 'INCOME')
        balance += t.amount;
      else
        balance -= t.amount;
    }
    return balance;
  }

  double get totalIncome => _transactions
      .where((t) => t.type == 'INCOME')
      .fold(0, (sum, item) => sum + item.amount);

  double get totalExpense => _transactions
      .where((t) => t.type == 'EXPENSE')
      .fold(0, (sum, item) => sum + item.amount);

  Future<void> fetchTransactions() async {
    _isLoading = true;
    notifyListeners();
    try {
      _transactions = await _apiService.getTransactions(currentUserId);
      // Sort by date descending
      _transactions.sort((a, b) => b.date.compareTo(a.date));
      _error = null;
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> addTransaction(TransactionModel transaction) async {
    try {
      final newTx =
          await _apiService.createTransaction(currentUserId, transaction);
      _transactions.insert(0, newTx);
      notifyListeners();
    } catch (e) {
      throw e;
    }
  }
}
