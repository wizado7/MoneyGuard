import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/app_theme.dart';
import '../../providers/ai_chat_provider.dart';
import 'package:intl/intl.dart';
import 'package:focus_detector/focus_detector.dart';

class AIChatScreen extends StatefulWidget {
  const AIChatScreen({super.key});

  @override
  State<AIChatScreen> createState() => _AIChatScreenState();
}

class _AIChatScreenState extends State<AIChatScreen> with WidgetsBindingObserver {
  final TextEditingController _messageController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final FocusNode _messageFocusNode = FocusNode();

  @override
  void initState() {
    super.initState();
    // Регистрируем наблюдатель
    WidgetsBinding.instance.addObserver(this);
    
    // Загружаем историю чата при открытии экрана
    Future.microtask(() {
      Provider.of<AIChatProvider>(context, listen: false).initialize();
    });
  }

  @override
  void dispose() {
    _messageController.dispose();
    _scrollController.dispose();
    _messageFocusNode.dispose();
    
    // Удаляем наблюдатель
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // Когда приложение возвращается на передний план
    if (state == AppLifecycleState.resumed) {
      // Убедимся, что клавиатура скрыта
      FocusManager.instance.primaryFocus?.unfocus();
      
      // Обновляем историю чата при возврате в приложение
      final chatProvider = Provider.of<AIChatProvider>(context, listen: false);
      chatProvider.refreshChatHistory();
    }
  }

  void _sendMessage() async {
    if (_messageController.text.trim().isEmpty) return;

    final message = _messageController.text;
    _messageController.clear();

    final chatProvider = Provider.of<AIChatProvider>(context, listen: false);
    await chatProvider.sendMessage(message);

    // Прокрутка к последнему сообщению
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final chatProvider = Provider.of<AIChatProvider>(context);
    
    return FocusDetector(
      onFocusGained: () {
        // Когда экран получает фокус, обновляем историю чата
        FocusManager.instance.primaryFocus?.unfocus();
        // Обновляем историю чата при возвращении на экран
        Future.microtask(() {
          Provider.of<AIChatProvider>(context, listen: false).refreshChatHistory();
        });
      },
      child: WillPopScope(
        // Обрабатываем нажатие кнопки "назад"
        onWillPop: () async {
          // Скрываем клавиатуру перед возвратом
          // Используем более надежный подход
          final currentFocus = FocusScope.of(context);
          if (!currentFocus.hasPrimaryFocus && currentFocus.focusedChild != null) {
            FocusManager.instance.primaryFocus?.unfocus();
          }
          return true;
        },
        child: GestureDetector(
          // Скрываем клавиатуру при нажатии вне полей ввода
          onTap: () => FocusScope.of(context).unfocus(),
          child: Scaffold(
            appBar: AppBar(
              title: Text('AI Ассистент'),
              automaticallyImplyLeading: false,
              leading: SizedBox.shrink(),
              actions: [
                IconButton(
                  icon: Icon(Icons.delete),
                  onPressed: () {
                    showDialog(
                      context: context,
                      builder: (context) => AlertDialog(
                        title: Text('Очистить историю'),
                        content: Text('Вы уверены, что хотите очистить историю чата?'),
                        actions: [
                          TextButton(
                            onPressed: () => Navigator.pop(context),
                            child: Text('Отмена'),
                          ),
                          TextButton(
                            onPressed: () {
                              chatProvider.clearMessages();
                              Navigator.pop(context);
                            },
                            child: Text('Очистить'),
                          ),
                        ],
                      ),
                    );
                  },
                ),
              ],
            ),
            body: Column(
              children: [
                // Сообщения
                Expanded(
                  child: chatProvider.messages.isEmpty
                      ? Center(
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(
                                Icons.chat_bubble_outline,
                                size: 64,
                                color: AppTheme.secondaryTextColor,
                              ),
                              SizedBox(height: 16),
                              Text(
                                'Задайте вопрос финансовому ассистенту',
                                style: TextStyle(
                                  color: AppTheme.secondaryTextColor,
                                  fontSize: 16,
                                ),
                                textAlign: TextAlign.center,
                              ),
                            ],
                          ),
                        )
                      : ListView.builder(
                          controller: _scrollController,
                          padding: EdgeInsets.all(16),
                          itemCount: chatProvider.messages.length,
                          itemBuilder: (context, index) {
                            final message = chatProvider.messages[index];
                            final isUser = message['isUser'];
                            final isError = message['isError'] ?? false;
                            
                            return _buildMessageBubble(
                              message['message'],
                              isUser,
                              message['timestamp'],
                              isError,
                            );
                          },
                        ),
                ),
                
                // Индикатор загрузки
                if (chatProvider.isLoading)
                  Padding(
                    padding: const EdgeInsets.all(8.0),
                    child: Row(
                      children: [
                        SizedBox(width: 16),
                        SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                          ),
                        ),
                        SizedBox(width: 16),
                        Text(
                          'Ассистент печатает...',
                          style: TextStyle(
                            color: AppTheme.secondaryTextColor,
                            fontSize: 14,
                          ),
                        ),
                      ],
                    ),
                  ),
                
                // Поле ввода
                Container(
                  padding: EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: AppTheme.cardColor,
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black12,
                        blurRadius: 4,
                        offset: Offset(0, -2),
                      ),
                    ],
                  ),
                  child: Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: _messageController,
                          focusNode: _messageFocusNode,
                          decoration: InputDecoration(
                            hintText: 'Введите сообщение...',
                            border: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(24),
                              borderSide: BorderSide.none,
                            ),
                            filled: true,
                            fillColor: AppTheme.secondaryCardColor,
                            contentPadding: EdgeInsets.symmetric(
                              horizontal: 16,
                              vertical: 8,
                            ),
                          ),
                          style: TextStyle(color: AppTheme.textColor),
                          maxLines: null,
                          textInputAction: TextInputAction.send,
                          onSubmitted: (_) => _sendMessage(),
                          enabled: !chatProvider.isLoading,
                        ),
                      ),
                      SizedBox(width: 8),
                      FloatingActionButton(
                        onPressed: chatProvider.isLoading ? null : _sendMessage,
                        mini: true,
                        backgroundColor: AppTheme.primaryColor,
                        foregroundColor: Colors.black,
                        child: Icon(Icons.send),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildMessageBubble(
      String message, bool isUser, DateTime timestamp, bool isError) {
    return Align(
      alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: EdgeInsets.only(
          top: 8,
          bottom: 8,
          left: isUser ? 64 : 0,
          right: isUser ? 0 : 64,
        ),
        padding: EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: isUser
              ? AppTheme.primaryColor
              : isError
                  ? Colors.red.withOpacity(0.2)
                  : AppTheme.cardColor,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              message,
              style: TextStyle(
                color: isUser
                    ? Colors.black
                    : isError
                        ? Colors.red
                        : AppTheme.textColor,
                fontSize: 16,
              ),
            ),
            SizedBox(height: 4),
            Text(
              DateFormat('HH:mm').format(timestamp),
              style: TextStyle(
                color: isUser
                    ? Colors.black.withOpacity(0.7)
                    : AppTheme.secondaryTextColor,
                fontSize: 12,
              ),
              textAlign: TextAlign.right,
            ),
          ],
        ),
      ),
    );
  }
} 