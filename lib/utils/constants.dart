import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

class AppColors {
  static const Color primary = Color(0xFF2E7D32); // The Green from Figma
  static const Color secondary = Color(0xFFE8F5E9);
  static const Color expense = Color(0xFFFFEBEE);
  static const Color expenseText = Color(0xFFD32F2F);
  static const Color income = Color(0xFFE8F5E9);
  static const Color incomeText = Color(0xFF388E3C);
  static const Color textDark = Color(0xFF1A1A1A);
  static const Color textLight = Color(0xFF757575);
}

// Backend URL
const String baseUrl = kReleaseMode ? '/api' : 'http://localhost:8080/api';
class CategoryUtils {
  static IconData getIconData(String name) {
    switch (name.toLowerCase()) {
      case 'food':
      case 'fastfood':
        return Icons.fastfood;
      case 'transport':
      case 'directions_bus':
        return Icons.directions_bus;
      case 'housing':
      case 'home':
        return Icons.home;
      case 'salary':
      case 'attach_money':
        return Icons.attach_money;
      case 'shopping':
      case 'shopping_cart':
        return Icons.shopping_cart;
      case 'health':
      case 'medical_services':
        return Icons.medical_services;
      case 'entertainment':
      case 'movie':
        return Icons.movie;
      case 'education':
      case 'school':
        return Icons.school;
      default:
        return Icons.category;
    }
  }
}
