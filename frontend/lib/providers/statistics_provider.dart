import 'package:flutter/foundation.dart';
import '../services/api_service.dart';

class StatisticsProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();
  
  Map<String, dynamic>? _statistics;
  bool _isLoading = false;
  String? _error;

  Map<String, dynamic>? get statistics => _statistics;
  bool get isLoading => _isLoading;
  String? get error => _error;

  Future<void> fetchStatistics({String? period}) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      _statistics = await _apiService.getStatistics(period: period);
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
    }
  }

  // Вспомогательные методы для получения конкретных данных из статистики
  double getTotalIncome() {
    return _statistics?['totalIncome'] ?? 0.0;
  }

  double getTotalExpense() {
    return _statistics?['totalExpense'] ?? 0.0;
  }

  double getBalance() {
    return _statistics?['balance'] ?? 0.0;
  }

  List<Map<String, dynamic>> getCategoryBreakdown() {
    if (_statistics == null || !_statistics!.containsKey('categoryBreakdown')) {
      return [];
    }
    
    final List<dynamic> data = _statistics!['categoryBreakdown'];
    return data.map((item) => item as Map<String, dynamic>).toList();
  }

  Map<String, double> getDailyExpenses() {
    if (_statistics == null || !_statistics!.containsKey('dailyExpenses')) {
      return {};
    }
    
    final Map<String, dynamic> data = _statistics!['dailyExpenses'];
    final Map<String, double> result = {};
    
    data.forEach((key, value) {
      result[key] = (value as num).toDouble();
    });
    
    return result;
  }

  Map<String, double> getMonthlyExpenses() {
    if (_statistics == null || !_statistics!.containsKey('monthlyExpenses')) {
      return {};
    }
    
    final Map<String, dynamic> data = _statistics!['monthlyExpenses'];
    final Map<String, double> result = {};
    
    data.forEach((key, value) {
      result[key] = (value as num).toDouble();
    });
    
    return result;
  }
} 