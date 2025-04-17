import 'package:json_annotation/json_annotation.dart';

part 'limit.g.dart';

@JsonSerializable()
class Limit {
  final int id;
  final String name;
  final double amount;
  final String period;
  final DateTime startDate;
  final DateTime endDate;
  final int categoryId;
  final String categoryName;
  final double currentUsage;
  final String? userId;

  Limit({
    required this.id,
    required this.name,
    required this.amount,
    required this.period,
    required this.startDate,
    required this.endDate,
    required this.categoryId,
    required this.categoryName,
    required this.currentUsage,
    this.userId,
  });

  factory Limit.fromJson(Map<String, dynamic> json) => _$LimitFromJson(json);

  Map<String, dynamic> toJson() => _$LimitToJson(this);

  double get percentageUsed => currentUsage / amount;
  
  double get remaining => amount - currentUsage;
} 