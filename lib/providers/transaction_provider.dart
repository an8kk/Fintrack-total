import 'package:flutter/material.dart';
import '../models/notification_model.dart';
import '../models/transaction_model.dart';
import '../services/api_service.dart';
import 'auth_provider.dart';

class TransactionProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();
  List<TransactionModel> _transactions = [];
  List<NotificationModel> _notifications = [];
  Map<String, dynamic>? _monthlyStats;
  String? _aiAnalysis;
  bool _isLoading = false;
  String? _error;
  int? _lastUserId;
  AuthProvider? _authProvider;

  List<TransactionModel> get transactions => _transactions;
  List<NotificationModel> get notifications => _notifications;
  Map<String, dynamic>? get monthlyStats => _monthlyStats;
  String? get aiAnalysis => _aiAnalysis;
  bool get isLoading => _isLoading;
  String? get error => _error;

  void update(AuthProvider authProvider) {
    if (_lastUserId != authProvider.currentUserId) {
      _lastUserId = authProvider.currentUserId;
      _transactions = [];
      _notifications = [];
      _monthlyStats = null;
      _aiAnalysis = null;
      _error = null;
      // notifyListeners is not strictly needed here as the proxy provider 
      // will trigger a rebuild if it detects a change, but clearing state 
      // prevents old data from flashing.
    }
    _authProvider = authProvider;
  }

  double get totalBalance {
    double balance = 0;
    for (var t in _transactions) {
      if (t.type == 'INCOME') {
        balance += t.amount;
      } else {
        balance -= t.amount;
      }
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
    if (_authProvider?.token == null || _authProvider?.currentUserId == null) return;
    _isLoading = true;
    notifyListeners();
    try {
      _transactions = await _apiService.getTransactions(
          _authProvider!.currentUserId!, _authProvider!.token!);
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
    if (_authProvider?.token == null || _authProvider?.currentUserId == null) return;
    try {
      final newTx = await _apiService.createTransaction(
          _authProvider!.currentUserId!, _authProvider!.token!, transaction);
      _transactions.insert(0, newTx);
      notifyListeners();
    } catch (e) {
      rethrow;
    }
  }

  Future<void> deleteTransaction(int id) async {
    try {
      await _apiService.deleteTransaction(id, _authProvider!.token!);
      _transactions.removeWhere((t) => t.id == id);
      notifyListeners();
    } catch (e) {
      rethrow;
    }
  }

  Future<void> fetchNotifications() async {
    if (_authProvider?.token == null) return;
    try {
      _notifications = await _apiService.getNotifications(
          _authProvider!.currentUserId!, _authProvider!.token!);
      notifyListeners();
    } catch (e) {
      _error = e.toString();
    }
  }

  Future<void> fetchStats(int month, int year) async {
    if (_authProvider?.token == null) return;
    try {
      _monthlyStats = await _apiService.getStats(
          _authProvider!.currentUserId!, _authProvider!.token!, month, year);
      notifyListeners();
    } catch (e) {
      _error = e.toString();
    }
  }

  Future<void> fetchAiAnalysis() async {
    if (_authProvider?.token == null || _authProvider?.currentUserId == null) return;
    try {
      _aiAnalysis = await _apiService.getAiAnalysis(
          _authProvider!.currentUserId!, _authProvider!.token!);
      notifyListeners();
    } catch (e) {
      _error = e.toString();
    }
  }

  Future<String> createSaltEdgeSession(String customerId) async {
    if (_authProvider?.token == null || _authProvider?.currentUserId == null) throw Exception("Not authenticated");
    try {
      return await _apiService.createSaltEdgeConnectSession(
          _authProvider!.currentUserId!, customerId, _authProvider!.token!);
    } catch (e) {
      rethrow;
    }
  }
}
