import 'package:flutter/foundation.dart';
import '../models/goal.dart';
import '../services/api_service.dart';
import '../providers/auth_provider.dart';
import 'package:intl/intl.dart';
import 'package:dio/dio.dart';

class GoalProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();
  AuthProvider? _authProvider;
  
  List<Goal> _goals = [];
  bool _isLoading = false;
  String? _error;

  List<Goal> get goals => _goals;
  bool get isLoading => _isLoading;
  String? get error => _error;

  void updateAuth(AuthProvider auth) {
    print("GoalProvider: Auth updated. IsAuthenticated: ${auth.isAuthenticated}");
    _authProvider = auth;
    if (auth.isAuthenticated && _goals.isEmpty && !_isLoading) {
      fetchGoals();
    } else if (!auth.isAuthenticated) {
      _goals = [];
      _error = null;
    }
  }

  Future<void> fetchGoals() async {
    if (!_authProvider!.isAuthenticated) {
      print("GoalProvider: Cannot fetch goals, user not authenticated.");
      return;
    }
    
    try {
      print("GoalProvider: Fetching goals...");
      _isLoading = true;
      _error = null;
      notifyListeners();
      
      final List<Goal> fetchedGoals = await _apiService.getGoals();
      _goals = fetchedGoals;
      
      // Сортируем цели по приоритету и прогрессу
      _goals.sort((a, b) {
        // Сначала по приоритету (высокий -> средний -> низкий)
        final priorityOrder = {'high': 0, 'medium': 1, 'low': 2};
        final aPriority = priorityOrder[a.priority ?? 'low'] ?? 2;
        final bPriority = priorityOrder[b.priority ?? 'low'] ?? 2;
        
        if (aPriority != bPriority) {
          return aPriority.compareTo(bPriority);
        }
        
        // Затем по прогрессу (меньший прогресс сверху)
        final aProgress = a.currentAmount / a.targetAmount;
        final bProgress = b.currentAmount / b.targetAmount;
        return aProgress.compareTo(bProgress);
      });
      
      print("GoalProvider: Goals fetched successfully (${_goals.length} items).");
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      print("GoalProvider: Error fetching goals: $e");
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> addGoal(Goal goal) async {
    if (_authProvider == null || !_authProvider!.isAuthenticated) return false;
    _isLoading = true;
    _error = null;
    notifyListeners();
    try {
      print("GoalProvider: Adding goal: ${goal.name}");
      final newGoal = await _apiService.createGoal(goal);
      _goals.add(newGoal);
      print("GoalProvider: Goal added successfully.");
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      print("GoalProvider: Error adding goal: $e");
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> updateGoal(Goal goal) async {
    if (!_authProvider!.isAuthenticated) return false;
    
    _isLoading = true;
    _error = null;
    notifyListeners();
    
    try {
      print("GoalProvider: Updating goal: ${goal.name} with current amount: ${goal.currentAmount}");
      final updatedGoal = await _apiService.updateGoal(goal);
      
      final index = _goals.indexWhere((g) => g.id == goal.id);
      if (index != -1) {
        _goals[index] = updatedGoal;
      }
      
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      print("GoalProvider: Error updating goal: $e");
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> deleteGoal(int id) async {
    if (_authProvider == null || !_authProvider!.isAuthenticated) return false;
    _isLoading = true;
    _error = null;
    notifyListeners();
    try {
      print("GoalProvider: Deleting goal with ID: $id");
      await _apiService.deleteGoal(id);
      _goals.removeWhere((g) => g.id == id);
      print("GoalProvider: Goal deleted successfully.");
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      print("GoalProvider: Error deleting goal: $e");
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }
} 