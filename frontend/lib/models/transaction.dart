import 'package:json_annotation/json_annotation.dart';
import 'package:intl/intl.dart';

part 'transaction.g.dart';

@JsonSerializable()
class Transaction {
  final int? id;
  
  @JsonKey(name: 'categoryId')
  final int categoryId;
  
  @JsonKey(name: 'category')
  final String? categoryName;
  
  final double amount;
  final String? description;
  
  @JsonKey(fromJson: _dateFromJson)
  final DateTime date;
  
  @JsonKey(name: 'goalId')
  final int? goalId;
  
  @JsonKey(name: 'userId')
  String? userId;
  
  @JsonKey(fromJson: _dateTimeFromJson)
  final DateTime? createdAt;

  double? amountToGoal;

  Transaction({
    this.id,
    required this.categoryId,
    this.categoryName,
    required this.amount,
    this.description,
    required this.date,
    this.goalId,
    this.userId,
    this.createdAt,
    this.amountToGoal,
    bool isIncome = false,
  });

  // Статический метод для преобразования строки в DateTime
  static DateTime _dateFromJson(dynamic date) {
    if (date is DateTime) return date;
    if (date is String) {
      try {
        // Пробуем разные форматы даты
        if (date.contains('T')) {
          return DateTime.parse(date);
        } else {
          // Если дата в формате YYYY-MM-DD
          final parts = date.split('-');
          if (parts.length == 3) {
            return DateTime(
              int.parse(parts[0]), 
              int.parse(parts[1]), 
              int.parse(parts[2])
            );
          }
        }
      } catch (e) {
        print('Ошибка при парсинге даты: $e');
      }
    }
    return DateTime.now(); // Возвращаем текущую дату в случае ошибки
  }
  
  // Статический метод для преобразования строки в DateTime?
  static DateTime? _dateTimeFromJson(dynamic date) {
    if (date == null) return null;
    if (date is DateTime) return date;
    if (date is String) {
      try {
        return DateTime.parse(date);
      } catch (e) {
        print('Ошибка при парсинге даты createdAt: $e');
      }
    }
    return null;
  }
  
  // Метод для сериализации даты в строку
  @JsonKey(toJson: _dateToJson)
  static String _dateToJson(DateTime date) {
    return date.toIso8601String().split('T')[0]; // Возвращаем только дату без времени
  }

  factory Transaction.fromJson(Map<String, dynamic> json) {
    try {
      String? parsedUserId;
      if (json['userId'] != null) {
        parsedUserId = json['userId'].toString();
      } else if (json['user_id'] != null) {
        parsedUserId = json['user_id'].toString();
      }

      var transaction = _$TransactionFromJson(json);
      transaction.userId = parsedUserId;
      transaction.amountToGoal = (json['amount_contributed_to_goal'] as num?)?.toDouble();
      return transaction;
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
        userId: userId,
        createdAt: json['createdAt'] != null ? DateTime.parse(json['createdAt']) : null,
        amountToGoal: (json['amount_contributed_to_goal'] as num?)?.toDouble(),
      );
    }
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = _$TransactionToJson(this);
    
    // Преобразуем дату в формат YYYY-MM-DD без времени
    if (date != null) {
      data['date'] = DateFormat('yyyy-MM-dd').format(date);
    }
    
    // Добавляем amountToGoal, если он задан
    if (amountToGoal != null) {
      data['amount_to_goal'] = amountToGoal;  // Используем snake_case для API
    }
    
    return data;
  }

  bool get isIncome => amount > 0;
} 