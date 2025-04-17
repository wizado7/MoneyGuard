import 'package:flutter/foundation.dart';
import '../models/auth_response.dart';
import '../services/api_service.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:moneyguard/constants/storage_keys.dart';

class AuthProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();
  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();
  
  String? _token;
  String? _refreshToken;
  bool _isAuthenticated = false;
  bool _isLoading = true;
  String? _error;
  AuthResponse? _authData;

  String? get token => _token;
  bool get isAuthenticated => _isAuthenticated;
  bool get isLoading => _isLoading;
  String? get error => _error;
  AuthResponse? get authData => _authData;

  AuthProvider() {
    print("AuthProvider initialized. Calling tryAutoLogin...");
    tryAutoLogin();
  }

  Future<void> tryAutoLogin() async {
    _isLoading = true;
    _error = null;
    notifyListeners();
    
    try {
      print("AuthProvider: Attempting to read stored tokens...");
      final storedToken = await _secureStorage.read(key: StorageKeys.jwtToken);
      final storedRefreshToken = await _secureStorage.read(key: StorageKeys.refreshToken);

      if (storedToken != null && storedToken.isNotEmpty) {
        print("AuthProvider: Found stored token.");
        _token = storedToken;
        _refreshToken = storedRefreshToken;
        _isAuthenticated = true;
        print("AuthProvider: Auto login successful.");
      } else {
        print("AuthProvider: No stored token found.");
        _isAuthenticated = false;
      }
    } catch (e) {
      print("AuthProvider: Error during auto login: $e");
      _isAuthenticated = false;
      _error = "Ошибка авто-входа: $e";
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> register(String email, String password, String name) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      print("AuthProvider: Attempting registration for $email");
      final response = await _apiService.register(email, password, name);
      
      _token = response.token;
      _refreshToken = response.refreshToken;
      _isAuthenticated = true;

      await _secureStorage.write(key: StorageKeys.jwtToken, value: _token);
      if (_refreshToken != null && _refreshToken!.isNotEmpty) {
        await _secureStorage.write(key: StorageKeys.refreshToken, value: _refreshToken);
      } else {
        await _secureStorage.delete(key: StorageKeys.refreshToken);
      }
      print("AuthProvider: Registration successful. Tokens stored.");
      
      _authData = response;
      await _secureStorage.write(key: 'user_id', value: _authData!.userId);
      await _secureStorage.write(key: 'user_email', value: _authData!.email);
      await _secureStorage.write(key: 'user_name', value: _authData!.name);
      await _secureStorage.write(key: 'user_ai_access', value: _authData!.aiAccessEnabled.toString());
      
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      print("AuthProvider: Registration failed: $e");
      _error = "Ошибка регистрации: ${e.toString()}";
      _isAuthenticated = false;
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> login(String email, String password) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      print("AuthProvider: Attempting login for $email");
      final response = await _apiService.login(email, password);
      
      _token = response.token;
      _refreshToken = response.refreshToken;
      _isAuthenticated = true;

      print("AuthProvider: Login successful. Storing tokens...");
      await _secureStorage.write(key: StorageKeys.jwtToken, value: _token);
      if (_refreshToken != null && _refreshToken!.isNotEmpty) {
        await _secureStorage.write(key: StorageKeys.refreshToken, value: _refreshToken);
      } else {
        await _secureStorage.delete(key: StorageKeys.refreshToken);
      }
      print("AuthProvider: Tokens stored successfully.");
      
      // Проверяем, что токен действительно сохранен
      final storedToken = await _secureStorage.read(key: StorageKeys.jwtToken);
      print("AuthProvider: Stored token: ${storedToken != null && storedToken.isNotEmpty ? 'OK' : 'NOT FOUND'}");
      
      _authData = response;
      await _secureStorage.write(key: 'user_id', value: _authData!.userId);
      await _secureStorage.write(key: 'user_email', value: _authData!.email);
      await _secureStorage.write(key: 'user_name', value: _authData!.name);
      await _secureStorage.write(key: 'user_ai_access', value: _authData!.aiAccessEnabled.toString());
      
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      print("AuthProvider: Login failed: $e");
      _error = e.toString();
      _isAuthenticated = false;
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<void> logout() async {
    print("AuthProvider: Logging out...");
    _token = null;
    _refreshToken = null;
    _isAuthenticated = false;
    _error = null;
    
    await _secureStorage.delete(key: StorageKeys.jwtToken);
    await _secureStorage.delete(key: StorageKeys.refreshToken);
    print("AuthProvider: Logged out. Tokens deleted.");
    
    _authData = null;
    _isLoading = false;
    notifyListeners();
  }

  Future<bool> changePassword(String currentPassword, String newPassword) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      // Проверка подключения к интернету
      var connectivityResult = await Connectivity().checkConnectivity();
      if (connectivityResult == ConnectivityResult.none) {
        throw Exception('Нет подключения к интернету');
      }

      await _apiService.changePassword(currentPassword, newPassword);
      
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  // Метод для обновления токена
  Future<bool> refreshToken() async {
    try {
      final refreshToken = await _secureStorage.read(key: StorageKeys.refreshToken);
      if (refreshToken == null) {
        return false;
      }
      
      // Здесь должен быть вызов API для обновления токена
      // Например: final newTokens = await _apiService.refreshToken(refreshToken);
      
      // Пока просто возвращаем true, так как у нас нет метода refreshToken в API
      return true;
    } catch (e) {
      return false;
    }
  }
} 