import 'package:json_annotation/json_annotation.dart';
import 'package:intl/intl.dart';

part 'transaction.g.dart';

@JsonSerializable(explicitToJson: true)
class Transaction {
  final int? id;
  
  @JsonKey(name: 'categoryId')
  final int categoryId;
  
  @JsonKey(name: 'category')
  final String? categoryName;
  
  final double amount;
  final String? description;
  
  @JsonKey(fromJson: _dateFromJson, toJson: _dateToJson)
  final DateTime date;
  
  @JsonKey(name: 'goalId')
  final int? goalId;
  
  @JsonKey(name: 'userId')
  final String userId;
  
  @JsonKey(name: 'createdAt', fromJson: _dateTimeFromJson, toJson: _dateTimeToJson)
  final DateTime? createdAt;

  @JsonKey(name: 'amount_to_goal')
  double? amountContributedToGoal;

  Transaction({
    this.id,
    required this.categoryId,
    this.categoryName,
    required this.amount,
    this.description,
    required this.date,
    this.goalId,
    required this.userId,
    this.createdAt,
    this.amountContributedToGoal,
  });

  // Статический метод для преобразования строки в DateTime
  static DateTime _dateFromJson(dynamic date) {
    if (date is DateTime) return date;
    if (date is String) {
      try {
        // Если дата содержит время (формат ISO 8601)
        if (date.contains('T')) {
          return DateTime.parse(date);
        } else {
          // Если дата в формате YYYY-MM-DD, добавляем время из createdAt
          final now = DateTime.now();
          final parsedDate = DateTime.parse(date);
          return DateTime(
            parsedDate.year,
            parsedDate.month,
            parsedDate.day,
            now.hour,
            now.minute,
            now.second,
          );
        }
      } catch (e) {
        print('Ошибка при парсинге даты: $e');
      }
    }
    return DateTime.now(); // Возвращаем текущую дату в случае ошибки
  }
  
  // Статический метод для преобразования строки в DateTime
  static DateTime _dateTimeFromJson(dynamic date) {
    if (date == null) return DateTime.now(); // Возвращаем текущую дату, если null
    if (date is DateTime) return date;
    if (date is String) {
      try {
        return DateTime.parse(date);
      } catch (e) {
        print('Ошибка при парсинге даты createdAt: $e');
      }
    }
    return DateTime.now(); // Возвращаем текущую дату в случае ошибки
  }
  
  // Метод для сериализации даты в строку
  static String _dateToJson(DateTime date) {
    return DateFormat('yyyy-MM-dd').format(date); // Возвращаем полную дату с временем
  }

  static String? _dateTimeToJson(DateTime? dateTime) {
    return dateTime?.toIso8601String();
  }

  factory Transaction.fromJson(Map<String, dynamic> json) {
    try {
      String? parsedUserId;
      if (json['userId'] != null) {
        parsedUserId = json['userId'].toString();
      } else if (json['user_id'] != null) {
        parsedUserId = json['user_id'].toString();
      }

      return Transaction(
        id: json['id'] is int ? json['id'] : 0,
        categoryId: json['categoryId'] is int ? json['categoryId'] : 0,
        categoryName: json['category'] as String?,
        amount: (json['amount'] as num?)?.toDouble() ?? 0.0,
        description: json['description'] as String?,
        date: _dateFromJson(json['date']),
        goalId: json['goalId'] ?? json['goal_id'],
        userId: parsedUserId ?? '',
        createdAt: _dateTimeFromJson(json['createdAt']),
        amountContributedToGoal: (json['amountContributedToGoal'] ?? json['amount_contributed_to_goal'] as num?)?.toDouble(),
      );
    } catch (e) {
      print('Ошибка при десериализации Transaction: $e');
      // Возвращаем базовую транзакцию в случае ошибки
      String? userId;
      if (json['userId'] != null) {
        userId = json['userId'].toString();
      } else if (json['user_id'] != null) {
        userId = json['user_id'].toString();
      }
      
      return Transaction(
        id: json['id'] is int ? json['id'] : 0,
        categoryId: json['categoryId'] is int ? json['categoryId'] : 0,
        amount: (json['amount'] as num?)?.toDouble() ?? 0.0,
        date: _dateFromJson(json['date']),
        description: json['description'] is String ? json['description'] : '',
        goalId: json['goalId'] ?? json['goal_id'],
        userId: userId ?? '',
        createdAt: json['createdAt'] != null ? DateTime.parse(json['createdAt']) : DateTime.now(),
        amountContributedToGoal: (json['amountContributedToGoal'] ?? json['amount_contributed_to_goal'] as num?)?.toDouble(),
      );
    }
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = _$TransactionToJson(this);
    
    // Явно указываем поле amountContributedToGoal
    if (amountContributedToGoal != null) {
      // Используем оба варианта имени поля для надежности
      data['amount_to_goal'] = amountContributedToGoal;
    }
    
    return data;
  }

  bool get isIncome => amount > 0;
} 