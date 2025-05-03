import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import '../models/transaction.dart';
import '../services/api_service.dart';
import 'package:moneyguard/providers/auth_provider.dart';
import '../providers/goal_provider.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';

class TransactionProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();
  
  List<Transaction> _transactions = [];
  bool _isLoading = false;
  String? _error;
  AuthProvider? _authProvider;
  GoalProvider? _goalProvider;

  List<Transaction> get transactions => _transactions;
  bool get isLoading => _isLoading;
  String? get error => _error;

  List<Transaction> get incomeTransactions => _transactions.where((t) => t.amount > 0).toList();
  List<Transaction> get expenseTransactions => _transactions.where((t) => t.amount < 0).toList();

  void update(AuthProvider auth, GoalProvider goals) {
    print("TransactionProvider: Auth updated. IsAuthenticated: ${auth.isAuthenticated}");
    _authProvider = auth;
    _goalProvider = goals;
    if (auth.isAuthenticated && _transactions.isEmpty && !_isLoading) {
       fetchTransactions();
    } else if (!auth.isAuthenticated) {
       _transactions = [];
       _error = null;
    }
  }

  void updateAuth(AuthProvider auth) {
    update(auth, _goalProvider ?? GoalProvider());
  }

  Future<void> fetchTransactions({String? dateFrom, String? dateTo, String? categoryName}) async {
    if (_authProvider == null || !_authProvider!.isAuthenticated) return;
    
    try {
      print("TransactionProvider: Fetching transactions...");
      _isLoading = true;
      _error = null;
      notifyListeners();
      
      final List<Transaction> transactions = await _apiService.getTransactions(
        dateFrom: dateFrom,
        dateTo: dateTo,
        categoryName: categoryName
      );
      
      // Сортируем транзакции по дате (новые сверху)
      transactions.sort((a, b) => b.date.compareTo(a.date));
      
      _transactions = transactions;
      print("TransactionProvider: Transactions fetched successfully (${_transactions.length} items).");
      
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      print("TransactionProvider: Error fetching transactions: $e");
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> addTransaction(Transaction transaction) async {
    if (_authProvider == null || !_authProvider!.isAuthenticated) return false;
    _isLoading = true;
    _error = null;
    notifyListeners();
    try {
      print("TransactionProvider: Adding transaction: ${transaction.description}");
      
      final createdTransaction = await _apiService.createTransaction(transaction);
      _transactions.add(createdTransaction);
      _sortTransactions();
      
      print("TransactionProvider: Transaction added successfully.");
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      print("TransactionProvider: Error adding transaction: $e");
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> updateTransaction(Transaction transaction) async {
    if (_authProvider == null || !_authProvider!.isAuthenticated) return false;
    _isLoading = true;
    _error = null;
    notifyListeners();
    try {
      print("TransactionProvider: Updating transaction ${transaction.id}...");
      print("Amount contributed to goal: ${transaction.amountContributedToGoal}");
      
      final updatedTransaction = await _apiService.updateTransaction(transaction);
      
      print("Response from API: amountContributedToGoal=${updatedTransaction.amountContributedToGoal}");
      
      // Обновляем локальный список
      final index = _transactions.indexWhere((t) => t.id == transaction.id);
      if (index != -1) {
        _transactions[index] = updatedTransaction;
        _sortTransactions();
        print("TransactionProvider: Transaction updated successfully.");
      } else {
        print("TransactionProvider: Updated transaction not found in local list.");
      }
      
      // Безопасно обновляем цели, если провайдер доступен
      if (_goalProvider != null) {
        try {
          print("TransactionProvider: Refreshing goals after updating transaction...");
          await _goalProvider!.fetchGoals();
          print("TransactionProvider: Goals refreshed successfully.");
        } catch (e) {
          print("TransactionProvider: Error refreshing goals: $e");
          // Не прерываем выполнение основного метода из-за ошибки обновления целей
        }
      }
      
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      print("TransactionProvider: Error updating transaction: $e");
      rethrow;
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> deleteTransaction(int id, [BuildContext? context]) async {
    if (_authProvider == null || !_authProvider!.isAuthenticated) return false;
    _isLoading = true;
    _error = null;
    notifyListeners();
    try {
      print("TransactionProvider: Deleting transaction $id...");
      await _apiService.deleteTransaction(id);
      _transactions.removeWhere((t) => t.id == id);
      print("TransactionProvider: Transaction deleted successfully.");

      // Безопасно обновляем цели, если провайдер доступен
      if (_goalProvider != null) {
        try {
          print("TransactionProvider: Refreshing goals after deleting transaction...");
          await _goalProvider!.fetchGoals();
          print("TransactionProvider: Goals refreshed successfully.");
        } catch (e) {
          print("TransactionProvider: Error refreshing goals: $e");
          // Не прерываем выполнение основного метода из-за ошибки обновления целей
        }
      }

      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      print("TransactionProvider: Error deleting transaction: $e");
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  double getTotalIncome() {
    return incomeTransactions.fold(0, (sum, transaction) => sum + transaction.amount);
  }

  double getTotalExpense() {
    return expenseTransactions.fold(0, (sum, transaction) => sum + transaction.amount.abs());
  }

  double getBalance() {
    return _transactions.fold(0, (sum, transaction) => sum + transaction.amount);
  }
  
  // Получение транзакций за текущий месяц
  List<Transaction> getCurrentMonthTransactions() {
    final now = DateTime.now();
    final startOfMonth = DateTime(now.year, now.month, 1);
    
    return _transactions.where((t) => t.date.isAfter(startOfMonth)).toList();
  }
  
  // Получение расходов за текущий месяц
  double getCurrentMonthExpenses() {
    return getCurrentMonthTransactions()
        .where((t) => t.amount < 0)
        .fold(0, (sum, t) => sum + t.amount.abs());
  }

  void _sortTransactions() {
    _transactions.sort((a, b) => b.date.compareTo(a.date));
  }
} 