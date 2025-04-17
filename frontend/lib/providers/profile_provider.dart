import 'package:flutter/foundation.dart';
import '../models/user_profile.dart';
import '../services/api_service.dart';
import '../providers/auth_provider.dart';

class ProfileProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();
  
  UserProfile? _profile;
  bool _isLoading = false;
  String? _error;
  AuthProvider? _authProvider;

  UserProfile? get profile => _profile;
  bool get isLoading => _isLoading;
  String? get error => _error;

  void updateAuth(AuthProvider auth) {
    print("ProfileProvider: Auth updated. IsAuthenticated: ${auth.isAuthenticated}");
    _authProvider = auth;
    if (auth.isAuthenticated && _profile == null && !_isLoading) {
      fetchProfile();
    } else if (!auth.isAuthenticated) {
      _profile = null;
      _error = null;
    }
  }

  Future<void> fetchProfile() async {
    if (_authProvider == null || !_authProvider!.isAuthenticated) {
       print("ProfileProvider: Cannot fetch profile, user not authenticated.");
       return;
    }
    print("ProfileProvider: Fetching profile...");
    _isLoading = true;
    _error = null;
    notifyListeners();
    try {
      _profile = await _apiService.getUserProfile();
      print("ProfileProvider: Profile fetched successfully.");
    } catch (e) {
      print("ProfileProvider: Error fetching profile: $e");
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> updateProfile(UserProfile updatedProfile) async {
    if (_authProvider == null || !_authProvider!.isAuthenticated) return false;
    _isLoading = true;
    _error = null;
    notifyListeners();
    try {
      print("ProfileProvider: Updating profile: ${updatedProfile.name}");
      _profile = await _apiService.updateUserProfile(updatedProfile.toJson());
      print("ProfileProvider: Profile updated successfully.");
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      print("ProfileProvider: Error updating profile: $e");
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }
} 