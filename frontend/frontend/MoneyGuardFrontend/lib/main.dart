import 'package:flutter/material.dart';
import 'presentation/screens/splash_screen.dart'; // استيراد شاشة البداية
import 'presentation/screens/welcome_screen.dart'; // استيراد شاشة الترحيب
import 'presentation/screens/HomeScreen.dart'; // استيراد الصفحة الرئيسية
import 'presentation/screens/login_screen.dart'; // استيراد شاشة تسجيل الدخول
import 'presentation/screens/register_screen.dart'; // استيراد شاشة تسجيل جديد

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'MoneyGuard',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      initialRoute: '/', // المسار الأولي للتطبيق
      routes: {
        '/': (context) => const SplashScreen(), // شاشة البداية
        '/welcome': (context) => const WelcomeScreen(), // شاشة الترحيب
        '/home': (context) => const HomeScreen(), // الصفحة الرئيسية
        '/login': (context) => const LoginScreen(), // شاشة تسجيل الدخول
        '/register': (context) => const RegisterScreen(), // شاشة تسجيل جديد
      },
    );
  }
}