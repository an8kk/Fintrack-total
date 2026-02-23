class NotificationModel {
  final int? id;
  final String title;
  final String message;
  final bool isRead;
  final DateTime date;

  NotificationModel({
    this.id,
    required this.title,
    required this.message,
    this.isRead = false,
    required this.date,
  });

  factory NotificationModel.fromJson(Map<String, dynamic> json) {
    return NotificationModel(
      id: json['id'],
      title: json['title'] ?? '',
      message: json['message'] ?? '',
      isRead: json['read'] ?? json['isRead'] ?? false,
      date: DateTime.parse(json['date']),
    );
  }
}
