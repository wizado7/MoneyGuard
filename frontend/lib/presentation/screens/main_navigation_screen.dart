import 'package:flutter/material.dart';
import 'HomeScreen.dart';
import 'AddTransactionScreen.dart';
import 'StatisticsScreen.dart';
import 'AIChatScreen.dart';
import 'package:focus_detector/focus_detector.dart';

class MainNavigationScreen extends StatefulWidget {
  const MainNavigationScreen({super.key});

  @override
  State<MainNavigationScreen> createState() => _MainNavigationScreenState();
}

class _MainNavigationScreenState extends State<MainNavigationScreen> with WidgetsBindingObserver {
  int _selectedIndex = 0;
  
  // Список экранов для отображения
  final List<Widget> _screens = [
    const HomeScreen(),
    const AddTransactionScreen(),
    const StatisticsScreen(),
    const AIChatScreen(),
  ];

  @override
  void initState() {
    super.initState();
    // Регистрируем наблюдатель
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
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
    }
  }

  void _onItemTapped(int index) {
    // Скрываем клавиатуру при переключении вкладок
    // Используем более надежный подход
    final currentFocus = FocusScope.of(context);
    if (!currentFocus.hasPrimaryFocus && currentFocus.focusedChild != null) {
      FocusManager.instance.primaryFocus?.unfocus();
    }
    
    // Если мы переключаемся с экрана добавления транзакций или чата,
    // делаем небольшую задержку для корректного скрытия клавиатуры
    if (_selectedIndex == 1 || _selectedIndex == 3) {
      // Добавляем небольшую задержку для корректного скрытия клавиатуры
      Future.delayed(Duration(milliseconds: 300), () {
        setState(() {
          _selectedIndex = index;
        });
      });
    } else {
      setState(() {
        _selectedIndex = index;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return FocusDetector(
      onFocusGained: () {
        // Когда экран получает фокус
        FocusManager.instance.primaryFocus?.unfocus();
      },
      child: Scaffold(
        body: IndexedStack(
          index: _selectedIndex,
          children: _screens,
        ),
        bottomNavigationBar: BottomNavigationBar(
          currentIndex: _selectedIndex,
          onTap: _onItemTapped,
          type: BottomNavigationBarType.fixed,
          items: const [
            BottomNavigationBarItem(
              icon: Icon(Icons.home_outlined),
              label: 'Главная',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.add),
              label: 'Добавить',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.bar_chart),
              label: 'Статистика',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.chat_bubble_outline),
              label: 'Чат',
            ),
          ],
        ),
      ),
    );
  }
} 