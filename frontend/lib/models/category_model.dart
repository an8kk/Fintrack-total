class CategoryModel {
  final int? id;
  final String name;
  final String icon;
  final String color;
  final String type; // "INCOME" or "EXPENSE"
  final double budgetLimit;

  CategoryModel({
    this.id,
    required this.name,
    required this.icon,
    required this.color,
    required this.type,
    this.budgetLimit = 0,
  });

  factory CategoryModel.fromJson(Map<String, dynamic> json) {
    return CategoryModel(
      id: json['id'],
      name: json['name'] ?? '',
      icon: json['icon'] ?? 'category',
      color: json['color'] ?? '0xFF9E9E9E',
      type: json['type'] ?? 'EXPENSE',
      budgetLimit: (json['budgetLimit'] ?? 0).toDouble(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'icon': icon,
      'color': color,
      'type': type,
      'budgetLimit': budgetLimit,
    };
  }
}
