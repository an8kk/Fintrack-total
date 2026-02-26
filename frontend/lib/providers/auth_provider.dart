import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../services/api_service.dart';

class AuthProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();

  String? _token;
  int? _currentUserId;
  String? _username;
  String? _email;
  String? _role;
  bool _isPremium = false;
  bool _isLoading = false;
  String? _error;

  String? get token => _token;
  int? get currentUserId => _currentUserId;
  String? get username => _username;
  String? get email => _email;
  String? get role => _role;
  bool get isLoading => _isLoading;
  bool get isAdmin => _role == 'ADMIN';
  bool get isPremium => _isPremium;
  String? get error => _error;
  bool get isAuthenticated => _token != null;

  /// Attempts to restore session from SharedPreferences.
  /// Returns true if a valid session was restored.
  Future<bool> tryAutoLogin() async {
    final prefs = await SharedPreferences.getInstance();
    final savedToken = prefs.getString('token');
    final savedUserId = prefs.getInt('userId');
    final savedUsername = prefs.getString('username');

    if (savedToken == null || savedUserId == null) return false;

    _token = savedToken;
    _currentUserId = savedUserId;
    _username = savedUsername;
    // Role will be fetched with profile

    try {
      await fetchUserProfile();
      return true;
    } catch (_) {
      // Token expired or invalid — clear and require fresh login
      await logout();
      return false;
    }
  }

  Future<void> login(String emailInput, String password) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final data = await _apiService.login(emailInput, password);
      _token = data['token'];
      _currentUserId = data['userId'];
      _username = data['username'];
      _role = data['role'];
      _isPremium = data['isPremium'] ?? false;

      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('token', _token!);
      await prefs.setInt('userId', _currentUserId!);
      await prefs.setString('username', _username!);

      await fetchUserProfile();
    } catch (e) {
      _error = e.toString();
      rethrow;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> fetchUserProfile() async {
    if (_token == null || _currentUserId == null) return;
    try {
      final data = await _apiService.getUserProfile(_currentUserId!, _token!);
      _username = data['username'];
      _email = data['email'];
      _role = data['role'];
      _isPremium = data['isPremium'] ?? false;
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      rethrow;
    }
  }

  Future<void> _updateLocalAuthData(
      String newToken, String newName, String newEmail) async {
    _token = newToken;
    _username = newName;
    _email = newEmail;

    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('token', newToken);
    await prefs.setString('username', newName);
  }

  Future<void> updateProfile(String newName, String newEmail) async {
    if (_token == null || _currentUserId == null) return;
    _isLoading = true;
    notifyListeners();

    try {
      final data = await _apiService.updateUser(
          _currentUserId!, _token!, newName, newEmail);

      await _updateLocalAuthData(
          data['token'], data['username'], data['email']);
    } catch (e) {
      _error = e.toString();
      rethrow;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> changePassword(String oldPassword, String newPassword) async {
    if (_token == null || _currentUserId == null) return;
    _isLoading = true;
    notifyListeners();

    try {
      final data = await _apiService.changePassword(
          _currentUserId!, _token!, oldPassword, newPassword);

      await _updateLocalAuthData(
          data['token'], data['username'], data['email']);
    } catch (e) {
      _error = e.toString();
      rethrow;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> register(String username, String email, String password, {String? saltEdgeCustomerId}) async {
    _isLoading = true;
    notifyListeners();
    try {
      await _apiService.register(username, email, password, saltEdgeCustomerId: saltEdgeCustomerId);
    } catch (e) {
      _error = e.toString();
      rethrow;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> upgradeToPremium(String cardNumber) async {
    if (_token == null) return;
    _isLoading = true;
    notifyListeners();
    try {
      final updatedData = await _apiService.upgradeToPremium(cardNumber, _token!);
      _isPremium = updatedData['isPremium'] ?? true;
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      rethrow;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> logout() async {
    _token = null;
    _currentUserId = null;
    _username = null;
    _email = null;
    _isPremium = false;
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();
    notifyListeners();
  }
}
