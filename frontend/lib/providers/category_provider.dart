import 'package:flutter/foundation.dart';
import '../models/category.dart' as models;
import '../services/api_service.dart';
import '../providers/auth_provider.dart';

class CategoryProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();
  AuthProvider? _authProvider;
  
  List<models.Category> _categories = [];
  bool _isLoading = false;
  String? _error;

  List<models.Category> get categories => _categories;
  bool get isLoading => _isLoading;
  String? get error => _error;

  List<models.Category> get incomeCategories => _categories.where((category) => category.isIncome).toList();
  List<models.Category> get expenseCategories => _categories.where((category) => !category.isIncome).toList();

  void updateAuth(AuthProvider auth) {
    print("CategoryProvider: Auth updated. IsAuthenticated: ${auth.isAuthenticated}");
    _authProvider = auth;
    if (auth.isAuthenticated && _categories.isEmpty && !_isLoading) {
       fetchCategories();
    } else if (!auth.isAuthenticated) {
       _categories = [];
       _error = null;
    }
  }

  Future<void> fetchCategories() async {
    if (_authProvider == null || !_authProvider!.isAuthenticated) {
       print("CategoryProvider: Cannot fetch categories, user not authenticated.");
       return;
    }
    print("CategoryProvider: Fetching categories...");
    _isLoading = true;
    _error = null;
    notifyListeners();
    try {
      _categories = await _apiService.getCategories();
      print("CategoryProvider: Categories fetched successfully (${_categories.length} items).");
    } catch (e) {
      print("CategoryProvider: Error fetching categories: $e");
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> createCategory(models.Category category) async {
    if (_authProvider == null || !_authProvider!.isAuthenticated) return false;
    _isLoading = true;
    _error = null;
    notifyListeners();
    try {
      print("CategoryProvider: Creating category: ${category.name}");
      final newCategory = await _apiService.createCategory(category);
      _categories.add(newCategory);
      print("CategoryProvider: Category created successfully.");
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      print("CategoryProvider: Error creating category: $e");
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> addCategory(models.Category category) async {
    return createCategory(category);
  }

  Future<bool> updateCategory(models.Category category) async {
    if (_authProvider == null || !_authProvider!.isAuthenticated) return false;
    _isLoading = true;
    _error = null;
    notifyListeners();
    try {
      print("CategoryProvider: Updating category: ${category.name}");
      final updatedCategory = await _apiService.updateCategory(category);
      final index = _categories.indexWhere((c) => c.id == category.id);
      if (index != -1) {
        _categories[index] = updatedCategory;
      }
      print("CategoryProvider: Category updated successfully.");
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      print("CategoryProvider: Error updating category: $e");
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> deleteCategory(int id) async {
    if (_authProvider == null || !_authProvider!.isAuthenticated) return false;
    _isLoading = true;
    _error = null;
    notifyListeners();
    try {
      print("CategoryProvider: Deleting category with ID: $id");
      await _apiService.deleteCategory(id);
      _categories.removeWhere((c) => c.id == id);
      print("CategoryProvider: Category deleted successfully.");
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      print("CategoryProvider: Error deleting category: $e");
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  models.Category? getCategoryById(int id) {
    try {
      return _categories.firstWhere((category) => category.id == id);
    } catch (e) {
      print("CategoryProvider: Category with id $id not found");
      return null;
    }
  }
} 