class TransactionModel {
  final int? id;
  final double amount;
  final String category;
  final String description;
  final DateTime date;
  final String type; // "INCOME" or "EXPENSE"

  TransactionModel({
    this.id,
    required this.amount,
    required this.category,
    required this.description,
    required this.date,
    required this.type,
  });

  factory TransactionModel.fromJson(Map<String, dynamic> json) {
    return TransactionModel(
      id: json['id'],
      amount: json['amount'],
      category: json['category'] ?? 'General',
      description: json['description'] ?? '',
      date: DateTime.parse(json['date']),
      type: json['type'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'amount': amount,
      'category': category,
      'description': description,
      'date': date.toIso8601String(),
      'type': type,
    };
  }
}
