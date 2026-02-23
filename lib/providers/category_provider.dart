import 'package:flutter/material.dart';
import '../models/category_model.dart';
import '../services/api_service.dart';
import 'auth_provider.dart';

class CategoryProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();
  List<CategoryModel> _categories = [];
  int? _lastUserId;
  AuthProvider? _authProvider;

  List<CategoryModel> get categories => _categories;

  void update(AuthProvider authProvider) {
    if (_lastUserId != authProvider.currentUserId) {
      _lastUserId = authProvider.currentUserId;
      _categories = [];
    }
    _authProvider = authProvider;
  }

  Future<void> fetchCategories() async {
    if (_authProvider?.token == null) return;
    try {
      _categories = await _apiService.getCategories(
          _authProvider!.currentUserId!, _authProvider!.token!);
      notifyListeners();
    } catch (e) {
      rethrow;
    }
  }

  Future<void> createCategory(
      String name, String icon, String color, double limit, String type) async {
    try {
      await _apiService.createCategory(_authProvider!.currentUserId!, _authProvider!.token!, {
        "name": name,
        "icon": icon,
        "color": color,
        "budgetLimit": limit,
        "type": type
      });
      await fetchCategories();
    } catch (e) {
      rethrow;
    }
  }

  Future<void> deleteCategory(int id) async {
    try {
      await _apiService.deleteCategory(id, _authProvider!.token!);
      _categories.removeWhere((c) => c.id == id);
      notifyListeners();
    } catch (e) {
      rethrow;
    }
  }

  IconData getIconData(String iconName) {
    switch (iconName) {
      case 'fastfood':
        return Icons.fastfood;
      case 'directions_bus':
        return Icons.directions_bus;
      case 'shopping_bag':
        return Icons.shopping_bag;
      case 'attach_money':
        return Icons.attach_money;
      case 'home':
        return Icons.home;
      case 'local_hospital':
        return Icons.local_hospital;
      case 'movie':
        return Icons.movie;
      case 'work':
        return Icons.work;
      default:
        return Icons.category;
    }
  }
}
