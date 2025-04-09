import 'package:flutter/material.dart';
import 'dart:async';
import 'welcome_screen.dart'; // استيراد شاشة الترحيب

class SplashScreen extends StatefulWidget {
  const SplashScreen({Key? key}) : super(key: key);

  @override
  _SplashScreenState createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  @override
  void initState() {
    super.initState();
    // مؤقت لمدة 3 ثوانٍ قبل الانتقال إلى شاشة الترحيب
    Timer(Duration(seconds: 3), () {
      Navigator.pushReplacementNamed(context, '/welcome'); // الانتقال إلى WelcomeScreen
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black, // خلفية سوداء
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Image.asset(
              'assets/images/Logo.png', // استبدل بالمسار الصحيح للصورة
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