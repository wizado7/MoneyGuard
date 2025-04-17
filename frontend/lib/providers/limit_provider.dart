import 'package:flutter/foundation.dart';
import '../models/limit.dart';
import '../services/api_service.dart';
import '../providers/auth_provider.dart';

class LimitProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();
  AuthProvider? _authProvider;
  
  List<Limit> _limits = [];
  bool _isLoading = false;
  String? _error;

  List<Limit> get limits => _limits;
  bool get isLoading => _isLoading;
  String? get error => _error;

  void updateAuth(AuthProvider auth) {
    print("LimitProvider: Auth updated. IsAuthenticated: ${auth.isAuthenticated}");
    _authProvider = auth;
    if (auth.isAuthenticated && _limits.isEmpty && !_isLoading) {
      fetchLimits();
    } else if (!auth.isAuthenticated) {
      _limits = [];
      _error = null;
    }
  }

  Future<void> fetchLimits() async {
    if (_authProvider == null || !_authProvider!.isAuthenticated) {
       print("LimitProvider: Cannot fetch limits, user not authenticated.");
       return;
    }
    print("LimitProvider: Fetching limits...");
    _isLoading = true;
    _error = null;
    notifyListeners();
    try {
      _limits = await _apiService.getLimits();
      print("LimitProvider: Limits fetched successfully (${_limits.length} items).");
    } catch (e) {
      print("LimitProvider: Error fetching limits: $e");
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> addLimit(Limit limit) async {
    if (_authProvider == null || !_authProvider!.isAuthenticated) return false;
    _isLoading = true;
    _error = null;
    notifyListeners();
    try {
      print("LimitProvider: Adding limit with ID: ${limit.id}");
      final newLimit = await _apiService.createLimit(limit);
      _limits.add(newLimit);
      print("LimitProvider: Limit added successfully.");
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      print("LimitProvider: Error adding limit: $e");
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> updateLimit(Limit limit) async {
    if (_authProvider == null || !_authProvider!.isAuthenticated) return false;
    _isLoading = true;
    _error = null;
    notifyListeners();
    try {
      print("LimitProvider: Updating limit: ${limit.name}");
      final updatedLimit = await _apiService.updateLimit(limit);
      final index = _limits.indexWhere((l) => l.id == limit.id);
      if (index != -1) {
        _limits[index] = updatedLimit;
      }
      print("LimitProvider: Limit updated successfully.");
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      print("LimitProvider: Error updating limit: $e");
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> deleteLimit(int id) async {
    if (_authProvider == null || !_authProvider!.isAuthenticated) return false;
    _isLoading = true;
    _error = null;
    notifyListeners();
    try {
      print("LimitProvider: Deleting limit with ID: $id");
      await _apiService.deleteLimit(id);
      _limits.removeWhere((l) => l.id == id);
      print("LimitProvider: Limit deleted successfully.");
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      print("LimitProvider: Error deleting limit: $e");
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }
} 