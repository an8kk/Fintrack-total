import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:http/http.dart' show MultipartFile;
import '../utils/constants.dart';
import '../models/transaction_model.dart';
import '../models/category_model.dart';
import '../models/notification_model.dart';
import '../models/user_model.dart';

class ApiService {
  // 1. LOGIN
  Future<Map<String, dynamic>> login(String email, String password) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/login'),
      headers: {"Content-Type": "application/json"},
      body: jsonEncode({"email": email, "password": password}),
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Login failed: ${response.body}');
    }
  }

  // 2. REGISTER
  Future<void> register(String username, String email, String password, {String? saltEdgeCustomerId}) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/register'),
      headers: {"Content-Type": "application/json"},
      body: jsonEncode(
          {"username": username, "email": email, "password": password, "saltEdgeCustomerId": saltEdgeCustomerId}),
    );

    if (response.statusCode != 200) {
      throw Exception('Registration failed: ${response.body}');
    }
  }

  // 2.1 FORGOT PASSWORD
  Future<void> forgotPassword(String email) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/forgot-password'),
      headers: {"Content-Type": "application/json"},
      body: jsonEncode({"email": email}),
    );
    if (response.statusCode != 200) {
      throw Exception('Forgot password request failed: ${response.body}');
    }
  }

  // 2.2 RESET PASSWORD
  Future<void> resetPassword(String token, String newPassword) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/reset-password'),
      headers: {"Content-Type": "application/json"},
      body: jsonEncode({"token": token, "newPassword": newPassword}),
    );
    if (response.statusCode != 200) {
      throw Exception('Reset password failed: ${response.body}');
    }
  }

  // 3. GET TRANSACTIONS
  Future<List<TransactionModel>> getTransactions(
      int userId, String token) async {
    final response = await http.get(
      Uri.parse('$baseUrl/transactions/$userId'),
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer $token",
      },
    );

    if (response.statusCode == 200) {
      List<dynamic> body = jsonDecode(response.body);
      return body.map((item) => TransactionModel.fromJson(item)).toList();
    } else {
      throw Exception('Failed to load transactions');
    }
  }

  // 4. CREATE TRANSACTION
  Future<TransactionModel> createTransaction(
      int userId, String token, TransactionModel transaction) async {
    final response = await http.post(
      Uri.parse('$baseUrl/transactions?userId=$userId'),
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer $token",
      },
      body: jsonEncode(transaction.toJson()),
    );

    if (response.statusCode == 200) {
      return TransactionModel.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Failed to create transaction: ${response.body}');
    }
  }

  // 4.1 UPDATE TRANSACTION
  Future<TransactionModel> updateTransaction(
      int userId, String token, int transactionId, TransactionModel transaction) async {
    final response = await http.put(
      Uri.parse('$baseUrl/transactions/$transactionId'),
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer $token",
      },
      body: jsonEncode(transaction.toJson()),
    );

    if (response.statusCode == 200) {
      return TransactionModel.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Failed to update transaction: ${response.body}');
    }
  }

  // 5. UPDATE USER PROFILE
  Future<Map<String, dynamic>> updateUser(
      int userId, String token, String name, String email) async {
    final response = await http.put(
      Uri.parse('$baseUrl/users/$userId'),
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer $token",
      },
      body: jsonEncode({
        "username": name,
        "email": email,
      }),
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to update profile: ${response.body}');
    }
  }

  // 6. CHANGE PASSWORD
  Future<Map<String, dynamic>> changePassword(
      int userId, String token, String oldPass, String newPass) async {
    final response = await http.put(
      Uri.parse('$baseUrl/users/$userId/password'),
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer $token",
      },
      body: jsonEncode({
        "oldPassword": oldPass,
        "newPassword": newPass,
      }),
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception(response.body);
    }
  }

  // 7. GET USER PROFILE
  Future<Map<String, dynamic>> getUserProfile(int userId, String token) async {
    final response = await http.get(
      Uri.parse('$baseUrl/users/$userId'),
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer $token",
      },
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to load profile');
    }
  }

  // GET CATEGORIES
  Future<List<CategoryModel>> getCategories(int userId, String token) async {
    final response = await http.get(
      Uri.parse('$baseUrl/data/categories/$userId'),
      headers: {"Authorization": "Bearer $token"},
    );
    if (response.statusCode == 200) {
      List<dynamic> body = jsonDecode(response.body);
      return body.map((item) => CategoryModel.fromJson(item)).toList();
    }
    throw Exception('Failed to load categories');
  }

  // GET NOTIFICATIONS
  Future<List<NotificationModel>> getNotifications(int userId, String token) async {
    final response = await http.get(
      Uri.parse('$baseUrl/data/notifications/$userId'),
      headers: {"Authorization": "Bearer $token"},
    );
    if (response.statusCode == 200) {
      List<dynamic> body = jsonDecode(response.body);
      return body.map((item) => NotificationModel.fromJson(item)).toList();
    }
    throw Exception('Failed to load notifications');
  }

  // GET MONTHLY STATS
  Future<Map<String, dynamic>> getStats(
      int userId, String token, int month, int year) async {
    final response = await http.get(
      Uri.parse('$baseUrl/data/stats/$userId?month=$month&year=$year'),
      headers: {"Authorization": "Bearer $token"},
    );
    if (response.statusCode == 200) return jsonDecode(response.body);
    throw Exception('Failed to load stats');
  }

  // DELETE TRANSACTION
  Future<void> deleteTransaction(int id, String token) async {
    final response = await http.delete(
      Uri.parse('$baseUrl/transactions/$id'),
      headers: {"Authorization": "Bearer $token"},
    );
    if (response.statusCode != 200) {
      throw Exception('Failed to delete transaction');
    }
  }

  // CREATE CATEGORY
  Future<void> createCategory(
      int userId, String token, Map<String, dynamic> categoryData) async {
    final response = await http.post(
      Uri.parse('$baseUrl/data/categories/$userId'),
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer $token"
      },
      body: jsonEncode(categoryData),
    );
    if (response.statusCode != 200) {
      throw Exception('Failed to create category');
    }
  }

  // DELETE CATEGORY
  Future<void> deleteCategory(int id, String token) async {
    final response = await http.delete(
      Uri.parse('$baseUrl/data/categories/$id'),
      headers: {"Authorization": "Bearer $token"},
    );
    if (response.statusCode != 200) {
      throw Exception('Failed to delete category');
    }
  }


  // ADMIN: GET ALL USERS
  Future<List<UserModel>> getAllUsers(String token) async {
    final response = await http.get(
      Uri.parse('$baseUrl/users'),
      headers: {"Authorization": "Bearer $token"},
    );

    if (response.statusCode == 200) {
      List<dynamic> body = jsonDecode(response.body);
      return body.map((item) => UserModel.fromJson(item)).toList();
    } else {
      throw Exception('Failed to load users');
    }
  }

  // ADMIN: TOGGLE USER STATUS
  Future<UserModel> toggleUserStatus(int userId, String token) async {
    final response = await http.put(
      Uri.parse('$baseUrl/users/$userId/status'),
      headers: {"Authorization": "Bearer $token"},
    );

    if (response.statusCode == 200) {
      return UserModel.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Failed to toggle user status');
    }
  }

  // ADMIN: UPDATE USER DETAILS
  Future<UserModel> updateUserByAdmin(
      int userId, String token, String name, String email) async {
    final response = await http.put(
      Uri.parse('$baseUrl/users/$userId/details'),
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer $token",
      },
      body: jsonEncode({
        "username": name,
        "email": email,
      }),
    );

    if (response.statusCode == 200) {
      return UserModel.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Failed to update user details: ${response.body}');
    }
  }

  // 9. AI ANALYSIS
  Future<String> getAiAnalysis(int userId, String token) async {
    final response = await http.get(
      Uri.parse('$baseUrl/data/stats/$userId/ai-analysis'),
      headers: {"Authorization": "Bearer $token"},
    );
    if (response.statusCode == 200) {
      return jsonDecode(response.body)['analysis'];
    } else {
      throw Exception('Failed to get AI analysis');
    }
  }

  // 10. SALT EDGE
  Future<String> createSaltEdgeConnectSession(int userId, String customerId, String token) async {
    final response = await http.post(
      Uri.parse('$baseUrl/saltedge/connect_sessions/create'),
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer $token"
      },
      body: jsonEncode({"customer_id": customerId}),
    );
    if (response.statusCode == 200) {
      return jsonDecode(response.body)['connect_url'];
    } else {
      throw Exception('Failed to create Salt Edge session');
    }
  }

  Future<void> importSaltEdgeTransactions(String token) async {
    final response = await http.post(
      Uri.parse('$baseUrl/saltedge/import'),
      headers: {"Authorization": "Bearer $token"},
    );
    if (response.statusCode != 200) {
      throw Exception('Failed to import Salt Edge transactions');
    }
  }

  Future<List<TransactionModel>> getSaltEdgeTransactions(String connectionId, String token) async {
    final response = await http.get(
      Uri.parse('$baseUrl/saltedge/transactions?connection_id=$connectionId'),
      headers: {"Authorization": "Bearer $token"},
    );
    if (response.statusCode == 200) {
      List<dynamic> body = jsonDecode(response.body);
      return body.map((item) => TransactionModel.fromJson(item)).toList();
    } else {
      throw Exception('Failed to fetch Salt Edge transactions');
    }
  }

  // 11. REPORTS
  Future<List<int>> exportReport(String token) async {
    final response = await http.get(
      Uri.parse('$baseUrl/reports/export'),
      headers: {"Authorization": "Bearer $token"},
    );
    if (response.statusCode != 200) {
      throw Exception('Failed to export report');
    }
    return response.bodyBytes;
  }

  Future<void> importReport(MultipartFile file, String token) async {
    // Note: Using http.MultipartRequest
    var request = http.MultipartRequest('POST', Uri.parse('$baseUrl/reports/import'));
    request.headers.addAll({"Authorization": "Bearer $token"});
    request.files.add(file);
    var response = await request.send();
    if (response.statusCode != 200) {
       throw Exception('Failed to import report');
    }
  }

  // 12. SALT EDGE STATUS & SYNC
  Future<Map<String, dynamic>> getSaltEdgeStatus(String token) async {
    final response = await http.get(
      Uri.parse('$baseUrl/saltedge/status'),
      headers: {"Authorization": "Bearer $token"},
    );
    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get Salt Edge status');
    }
  }

  Future<Map<String, dynamic>> syncSaltEdgeTransactions(String token) async {
    final response = await http.post(
      Uri.parse('$baseUrl/saltedge/sync'),
      headers: {"Authorization": "Bearer $token"},
    );
    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to sync transactions');
    }
  }

  // 13. AI INSIGHTS
  Future<List<dynamic>> getInsights(int userId, String token) async {
    final response = await http.get(
      Uri.parse('$baseUrl/data/stats/$userId/insights'),
      headers: {"Authorization": "Bearer $token"},
    );
    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get insights');
    }
  }

  // 14. GRANULAR AI INSIGHTS
  Future<String> getTransactionInsight(int userId, int transactionId, String token) async {
    final response = await http.post(
      Uri.parse('$baseUrl/data/stats/$userId/ai/transaction'),
      headers: {"Authorization": "Bearer $token", "Content-Type": "application/json"},
      body: jsonEncode({"transactionId": transactionId}),
    );
    if (response.statusCode == 200) {
      return jsonDecode(response.body)['insight'];
    } else {
      throw Exception('Failed to get transaction insight');
    }
  }

  Future<String> getCategoryInsight(int userId, String category, String token) async {
    final response = await http.post(
      Uri.parse('$baseUrl/data/stats/$userId/ai/category'),
      headers: {"Authorization": "Bearer $token", "Content-Type": "application/json"},
      body: jsonEncode({"category": category}),
    );
    if (response.statusCode == 200) {
      return jsonDecode(response.body)['insight'];
    } else {
      throw Exception('Failed to get category insight');
    }
  }

  Future<String> getPeriodInsight(int userId, String period, String date, String token) async {
    final response = await http.post(
      Uri.parse('$baseUrl/data/stats/$userId/ai/period'),
      headers: {"Authorization": "Bearer $token", "Content-Type": "application/json"},
      body: jsonEncode({"period": period, "date": date}),
    );
    if (response.statusCode == 200) {
      return jsonDecode(response.body)['insight'];
    } else {
      throw Exception('Failed to get period insight');
    }
  }
}
