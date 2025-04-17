import 'package:flutter/foundation.dart';
import '../services/api_service.dart';

class AIChatProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();
  
  List<Map<String, dynamic>> _messages = [];
  bool _isLoading = false;
  String? _error;

  List<Map<String, dynamic>> get messages => _messages;
  bool get isLoading => _isLoading;
  String? get error => _error;

  void addUserMessage(String message) {
    _messages.add({
      'isUser': true,
      'message': message,
      'timestamp': DateTime.now(),
    });
    notifyListeners();
  }

  Future<void> sendMessage(String message) async {
    addUserMessage(message);
    
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final response = await _apiService.sendMessageToAI(message);
      
      _messages.add({
        'isUser': false,
        'message': response,
        'timestamp': DateTime.now(),
      });
      
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      
      _messages.add({
        'isUser': false,
        'message': 'Извините, произошла ошибка. Пожалуйста, попробуйте позже.',
        'timestamp': DateTime.now(),
        'isError': true,
      });
      
      _isLoading = false;
      notifyListeners();
    }
  }

  void clearMessages() {
    _messages = [];
    notifyListeners();
  }
} 