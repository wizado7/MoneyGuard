import 'package:json_annotation/json_annotation.dart';
import 'package:intl/intl.dart';

part 'goal.g.dart';

@JsonSerializable()
class Goal {
  final int? id;
  final String name;
  final double targetAmount;
  final double currentAmount;
  final DateTime? targetDate;
  final String? priority;
  final double progress;
  final int? userId;

  Goal({
    this.id,
    required this.name,
    required this.targetAmount,
    this.currentAmount = 0,
    this.targetDate,
    this.priority,
    required this.progress,
    this.userId,
  });

  factory Goal.fromJson(Map<String, dynamic> json) {
    double targetAmount = 0;
    double currentAmount = 0;
    double progress = 0;

    if (json.containsKey('target_amount')) {
      targetAmount = double.parse(json['target_amount'].toString());
    } else if (json.containsKey('targetAmount')) {
      targetAmount = double.parse(json['targetAmount'].toString());
    }
    
    if (json.containsKey('current_amount')) {
      currentAmount = double.parse(json['current_amount'].toString());
    } else if (json.containsKey('currentAmount')) {
      currentAmount = double.parse(json['currentAmount'].toString());
    }
    
    if (json.containsKey('progress')) {
      progress = double.parse(json['progress'].toString());
    } else if (targetAmount > 0) {
      progress = currentAmount / targetAmount;
    }

    DateTime? targetDate;
    if (json.containsKey('target_date') && json['target_date'] != null && json['target_date'].toString().isNotEmpty) {
      try {
        targetDate = DateTime.parse(json['target_date'].toString());
      } catch (e) {
        print('Error parsing target date: $e');
      }
    } else if (json.containsKey('targetDate') && json['targetDate'] != null && json['targetDate'].toString().isNotEmpty) {
      try {
        targetDate = DateTime.parse(json['targetDate'].toString());
      } catch (e) {
        print('Error parsing target date: $e');
      }
    }

    return Goal(
      id: json['id'],
      name: json['name'] ?? '',
      targetAmount: targetAmount,
      currentAmount: currentAmount,
      targetDate: targetDate,
      priority: json['priority'],
      progress: progress,
      userId: json['userId'] ?? json['user_id'],
    );
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = {
      'name': name,
      'target_amount': targetAmount,
    };
    
    if (id != null) {
      data['id'] = id;
    }
    
    if (currentAmount > 0) {
      data['current_amount'] = currentAmount;
    }
    
    if (targetDate != null) {
      data['target_date'] = DateFormat('yyyy-MM-dd').format(targetDate!);
    }
    
    if (priority != null && priority!.isNotEmpty) {
      data['priority'] = priority;
    }
    
    if (userId != null) {
      data['user_id'] = userId;
    }
    
    return data;
  }

  // Вычисляемые свойства
  int get daysLeft => targetDate?.difference(DateTime.now()).inDays ?? 0;
  
  double get dailyContribution {
    final days = daysLeft > 0 ? daysLeft : 1;
    return (targetAmount - currentAmount) / days;
  }
  
  double get monthlyContribution {
    return dailyContribution * 30;
  }
} 