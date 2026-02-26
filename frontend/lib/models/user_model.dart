class UserModel {
  final int id;
  final String username;
  final String email;
  final String? role;
  final bool isBlocked;
  final bool isPremium;
  final double balance; // Added balance

  UserModel({
    required this.id,
    required this.username,
    required this.email,
    this.role,
    this.isBlocked = false,
    this.isPremium = false,
    this.balance = 0.0,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: json['id'],
      username: json['username'],
      email: json['email'],
      role: json['role'],
      isBlocked: json['isBlocked'] ?? false,
      isPremium: json['isPremium'] ?? false,
      balance: (json['balance'] ?? 0).toDouble(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'username': username,
      'email': email,
      'role': role,
      'isBlocked': isBlocked,
      'isPremium': isPremium,
      'balance': balance,
    };
  }
}
