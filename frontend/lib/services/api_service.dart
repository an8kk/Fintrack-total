import 'dart:convert';
import 'package:http/http.dart' as http;
import '../utils/constants.dart';
import '../models/transaction_model.dart';

class ApiService {
  // Fetch transactions for a specific user
  Future<List<TransactionModel>> getTransactions(int userId) async {
    final response = await http.get(Uri.parse('$baseUrl/$userId'));

    if (response.statusCode == 200) {
      List<dynamic> body = jsonDecode(response.body);
      return body.map((dynamic item) => TransactionModel.fromJson(item)).toList();
    } else {
      throw Exception('Failed to load transactions');
    }
  }

  // Create a new transaction
  Future<TransactionModel> createTransaction(int userId, TransactionModel transaction) async {
    final response = await http.post(
      Uri.parse('$baseUrl?userId=$userId'),
      headers: {"Content-Type": "application/json"},
      body: jsonEncode(transaction.toJson()),
    );

    if (response.statusCode == 200) {
      return TransactionModel.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Failed to create transaction: ${response.body}');
    }
  }
}