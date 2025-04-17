import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'dart:async';
import '../../providers/auth_provider.dart';

class SplashScreen extends StatefulWidget {
  const SplashScreen({Key? key}) : super(key: key);

  @override
  _SplashScreenState createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  @override
  void initState() {
    super.initState();
    // Проверяем аутентификацию и перенаправляем пользователя
    _checkAuthAndNavigate();
  }

  Future<void> _checkAuthAndNavigate() async {
    // Задержка для отображения сплеш-экрана
    await Future.delayed(Duration(seconds: 2));
    
    if (!mounted) return;
    
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    
    // Ждем завершения проверки аутентификации
    while (authProvider.isLoading) {
      await Future.delayed(Duration(milliseconds: 100));
    }
    
    // Если пользователь уже авторизован, перенаправляем на главный экран
    // Иначе на экран приветствия
    if (authProvider.isAuthenticated) {
      Navigator.pushReplacementNamed(context, '/home');
    } else {
      Navigator.pushReplacementNamed(context, '/welcome');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Image.asset(
              'assets/images/Logo.png',
              width: 80,
              height: 80,
            ),
            SizedBox(height: 20),
            Text(
              'MONEYGUARD',
              style: TextStyle(
                color: Colors.white,
                fontSize: 24,
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ),
      ),
    );
  }
}