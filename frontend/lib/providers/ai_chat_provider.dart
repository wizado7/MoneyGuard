import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'dart:convert';
import '../services/api_service.dart';

class AIChatProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();
  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();
  
  List<Map<String, dynamic>> _messages = [];
  bool _isLoading = false;
  bool _isLoadingHistory = false;
  String? _error;

  List<Map<String, dynamic>> get messages => _messages;
  bool get isLoading => _isLoading;
  bool get isLoadingHistory => _isLoadingHistory;
  String? get error => _error;

  // Инициализация - загружаем историю при запуске
  Future<void> initialize() async {
    print('AIChatProvider: Initializing...');
    // Сначала загружаем локальную историю для быстрого отображения
    await _loadMessagesLocally();
    notifyListeners();
    
    // Затем пытаемся загрузить серверную историю и объединить с локальной
    try {
      await loadChatHistory();
    } catch (e) {
      print('AIChatProvider: Failed to load server history during initialize: $e');
      // Не критично - локальная история уже загружена
    }
  }

  // Загружаем историю чата с сервера
  Future<void> loadChatHistory() async {
    print('AIChatProvider: Loading chat history from server...');
    _isLoadingHistory = true;
    _error = null;
    notifyListeners();

    try {
      final history = await _apiService.getChatHistory();
      print('AIChatProvider: Received ${history.length} messages from server');
      
      final serverMessages = <Map<String, dynamic>>[];
      
      for (var item in history) {
        // Проверяем роль и получаем соответствующее поле
        bool isUser = item['role'] == 'USER';
        String? message;
        
        if (isUser) {
          message = item['message'] as String?;
        } else {
          message = item['response'] as String?;
        }
        
        // Пропускаем пустые сообщения
        if (message == null || message.trim().isEmpty) {
          print('AIChatProvider: Skipping empty message for role ${item['role']}');
          continue;
        }
        
        final messageMap = {
          'isUser': isUser,
          'message': message,
          'timestamp': DateTime.tryParse(item['createdAt'] ?? '') ?? DateTime.now(),
          'isError': false,
        };
        
        serverMessages.add(messageMap);
        print('AIChatProvider: Added ${isUser ? 'user' : 'AI'} message: ${message.substring(0, message.length.clamp(0, 50))}...');
      }
      
      // Объединяем серверные и локальные сообщения
      final localMessages = List<Map<String, dynamic>>.from(_messages);
      
      // Создаем объединенный список без дубликатов на основе времени
      final allMessages = <Map<String, dynamic>>[];
      final messageSet = <String>{}; // Для отслеживания уникальности
      
      // Добавляем ВСЕ сообщения (серверные и локальные) в один список
      final combinedMessages = <Map<String, dynamic>>[];
      combinedMessages.addAll(serverMessages);
      combinedMessages.addAll(localMessages);
      
      // Сначала сортируем ВСЕ сообщения по времени
      combinedMessages.sort((a, b) => (a['timestamp'] as DateTime).compareTo(b['timestamp'] as DateTime));
      
      // Затем удаляем дубликаты, оставляя более поздние версии
      for (var msg in combinedMessages) {
        final time = msg['timestamp'] as DateTime;
        final text = msg['message'] as String;
        final isUser = msg['isUser'] as bool;
        final userPrefix = isUser ? 'USER' : 'AI';
        
        // Создаем уникальный ключ на основе роли и текста сообщения
        final messageKey = '$userPrefix:${text.trim()}';
        
        if (!messageSet.contains(messageKey)) {
          messageSet.add(messageKey);
          allMessages.add(msg);
          print('AIChatProvider: Added ${isUser ? 'user' : 'AI'} message: ${text.substring(0, text.length.clamp(0, 50))}...');
        } else {
          print('AIChatProvider: Skipped duplicate message: ${text.substring(0, text.length.clamp(0, 30))}...');
        }
      }
      
      _messages = allMessages;
      
      // Сохраняем объединенную историю локально
      await _saveMessagesLocally();
      
      _isLoadingHistory = false;
      notifyListeners();
      print('AIChatProvider: Chat history loaded successfully. Total messages: ${_messages.length}');
      
    } catch (e) {
      print('AIChatProvider: Error loading chat history: $e');
      _error = e.toString();
      _isLoadingHistory = false;
      
      // Если ошибка доступа, просто используем локальную историю
      if (e.toString().contains('403') || 
          e.toString().contains('AI доступ отключен')) {
        _error = null;
        print('AIChatProvider: Using local history due to access restriction');
      }
      
      notifyListeners();
    }
  }

  // Принудительная перезагрузка истории (например, после создания транзакции)
  Future<void> refreshChatHistory() async {
    print('AIChatProvider: Refreshing chat history...');
    await loadChatHistory();
  }

  // Принудительное обновление с очисткой локального кэша
  Future<void> forceRefreshHistory() async {
    print('AIChatProvider: Force refreshing chat history (clearing local cache)...');
    // Очищаем локальную историю
    _messages.clear();
    // Загружаем с сервера
    await loadChatHistory();
  }

  // Сохраняем сообщения локально
  Future<void> _saveMessagesLocally() async {
    try {
      print('AIChatProvider: Saving ${_messages.length} messages locally');
      final messagesJson = _messages.map((msg) => {
        'isUser': msg['isUser'],
        'message': msg['message'],
        'timestamp': (msg['timestamp'] as DateTime).toIso8601String(),
        'isError': msg['isError'] ?? false,
      }).toList();
      
      await _secureStorage.write(
        key: 'chat_history', 
        value: jsonEncode(messagesJson)
      );
      print('AIChatProvider: Messages saved locally successfully');
    } catch (e) {
      print('AIChatProvider: Error saving messages locally: $e');
    }
  }

  // Загружаем сообщения локально
  Future<void> _loadMessagesLocally() async {
    try {
      final savedMessages = await _secureStorage.read(key: 'chat_history');
      if (savedMessages != null) {
        final messagesList = jsonDecode(savedMessages) as List;
        _messages = messagesList.map<Map<String, dynamic>>((msg) => {
          'isUser': msg['isUser'] as bool,
          'message': msg['message'] as String,
          'timestamp': DateTime.parse(msg['timestamp'] as String),
          'isError': msg['isError'] ?? false,
        }).toList();
        print('AIChatProvider: Loaded ${_messages.length} messages from local storage');
      } else {
        _messages = [];
        print('AIChatProvider: No local messages found');
      }
    } catch (e) {
      print('AIChatProvider: Error loading local messages: $e');
      _messages = [];
    }
  }

  void addUserMessage(String message) {
    print('AIChatProvider: Adding user message: $message');
    final userMessage = {
      'isUser': true,
      'message': message,
      'timestamp': DateTime.now(),
      'isError': false,
    };
    
    _messages.add(userMessage);
    print('AIChatProvider: Total messages after adding user message: ${_messages.length}');
    _saveMessagesLocally(); // Сохраняем локально сразу
    notifyListeners();
  }

  void addAIMessage(String message) {
    print('AIChatProvider: Adding AI message: ${message.substring(0, message.length.clamp(0, 50))}...');
    final aiMessage = {
      'isUser': false,
      'message': message,
      'timestamp': DateTime.now(),
      'isError': false,
    };
    
    _messages.add(aiMessage);
    print('AIChatProvider: Total messages after adding AI message: ${_messages.length}');
    _saveMessagesLocally(); // Сохраняем локально сразу
    notifyListeners();
  }

  Future<void> sendMessage(String message) async {
    print('AIChatProvider: Sending message: $message');
    
    // Добавляем сообщение пользователя локально
    addUserMessage(message);
    
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final response = await _apiService.sendMessageToAI(message);
      print('AIChatProvider: Received AI response: ${response.substring(0, response.length.clamp(0, 100))}...');
      
      // Добавляем AI ответ локально
      addAIMessage(response);
      
      _isLoading = false;
      notifyListeners();
      
      print('AIChatProvider: Message exchange completed successfully');
      
    } catch (e) {
      print('AIChatProvider: Error sending message: $e');
      _error = e.toString();
      
      String errorMessage = 'Извините, произошла ошибка. Пожалуйста, попробуйте позже.';
      
      // Проверяем тип ошибки для более понятного сообщения
      if (e.toString().contains('403') || 
          e.toString().contains('Forbidden') ||
          e.toString().contains('AI доступ отключен')) {
        errorMessage = '🔒 AI доступ отключен для вашего аккаунта.\n\n'
                      '💡 Для получения доступа к AI чату обратитесь к администратору '
                      'или активируйте AI функции в настройках профиля.';
      } else if (e.toString().contains('401') || 
                 e.toString().contains('Unauthorized')) {
        errorMessage = 'Необходимо войти в аккаунт для использования AI чата.';
      } else if (e.toString().contains('400') || 
                 e.toString().contains('Bad Request')) {
        errorMessage = 'Проверьте правильность введенного сообщения.';
      } else if (e.toString().contains('network') || 
                 e.toString().contains('connection')) {
        errorMessage = 'Проблема с подключением к интернету. '
                      'Проверьте соединение и попробуйте снова.';
      } else if (e.toString().contains('timeout')) {
        errorMessage = 'Превышено время ожидания ответа. '
                      'Попробуйте отправить сообщение еще раз.';
      }
      
      // Добавляем сообщение об ошибке в чат
      final errorMessageObj = {
        'isUser': false,
        'message': errorMessage,
        'timestamp': DateTime.now(),
        'isError': true,
      };
      
      _messages.add(errorMessageObj);
      await _saveMessagesLocally(); // Сохраняем локально
      
      _isLoading = false;
      notifyListeners();
    }
  }

  // Обновляем историю (вызывается после создания транзакции)
  Future<void> refreshHistory() async {
    print('AIChatProvider: refreshHistory called - refreshing from server');
    // Принудительно загружаем историю с сервера для получения новых рекомендаций
    await loadChatHistory();
  }

  // Очищаем все сообщения
  void clearMessages() {
    print('AIChatProvider: Clearing all messages');
    _messages.clear();
    _secureStorage.delete(key: 'chat_history'); // Очищаем локальное хранилище
    notifyListeners();
  }
}