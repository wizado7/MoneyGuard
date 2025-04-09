import 'dart:async';

import 'package:flutter/material.dart';
import '../widgets/circle_indicator.dart'; // استيراد CircleIndicator
import 'login_screen.dart'; // استيراد شاشة تسجيل الدخول

class WelcomeScreen extends StatefulWidget {
  const WelcomeScreen({super.key});

  @override
  _WelcomeScreenState createState() => _WelcomeScreenState();
}

class _WelcomeScreenState extends State<WelcomeScreen> {
  int currentStep = 0; // مؤشر لتتبع الجزء الحالي

  // قائمة بالنصوص التي سيتم عرضها لكل جزء
  List<Map<String, String>> content = [
    {
      'title': 'Начните',
      'description': 'Управляйте своими финансами грамотно и четко',
    },
    {
      'title': 'Начните',
      'description': 'Используйте ии-ассистента для экономии',
    },
    {
      'title': 'Начните',
      'description': 'Стройте удобные цели и задачи',
    },
  ];

  void navigateToNextStep() {
    if (currentStep < content.length - 1) {
      setState(() {
        currentStep++;
      });
    } else {
      Navigator.pushReplacementNamed(context, '/login'); // الانتقال إلى شاشة تسجيل الدخول
    }
  }

  @override
  void initState() {
    super.initState();
    Timer.periodic(Duration(seconds: 3), (timer) {
      if (currentStep < content.length) {
        navigateToNextStep();
      } else {
        timer.cancel(); // إيقاف التكرار عند انتهاء جميع الأجزاء
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black, // خلفية سوداء
      body: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24), // مسافة جانبية
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              // العنوان الرئيسي
              Text(
                content[currentStep]['title']!,
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 28,
                  fontWeight: FontWeight.bold,
                ),
              ),

              // المسافة بين العنوان والوصف
              SizedBox(height: 10),

              // الوصف
              Text(
                content[currentStep]['description']!,
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                ),
                textAlign: TextAlign.center,
              ),

              // المسافة بين الوصف والدوائر
              SizedBox(height: 50),

              // دوائر الخطوات
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  CircleIndicator(isActive: currentStep == 0),
                  SizedBox(width: 8),
                  CircleIndicator(isActive: currentStep == 1),
                  SizedBox(width: 8),
                  CircleIndicator(isActive: currentStep == 2),
                ],
              ),

              // المسافة بين الدوائر والأزرار
              SizedBox(height: 50),

              // الزر الأول: تسجيل الدخول
              ElevatedButton(
                onPressed: () {
                  Navigator.pushReplacementNamed(context, '/login');
                },
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.white, // الخلفية البيضاء
                  foregroundColor: Colors.black, // النص الأسود
                  padding: EdgeInsets.symmetric(vertical: 16), // مسافة داخلية
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8), // زوايا مستديرة
                  ),
                  minimumSize: Size(double.infinity, 50), // عرض كامل
                ),
                child: Text(
                  'Войти',
                  style: TextStyle(fontSize: 18),
                ),
              ),

              // المسافة بين الأزرار
              SizedBox(height: 16),

              // الزر الثاني: تسجيل جديد
              ElevatedButton(
                onPressed: () {
                  Navigator.pushReplacementNamed(context, '/register');
                },
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.grey[700], // الخلفية الرمادية المظلمة
                  foregroundColor: Colors.white, // النص الأبيض
                  padding: EdgeInsets.symmetric(vertical: 16), // مسافة داخلية
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8), // زوايا مستديرة
                  ),
                  minimumSize: Size(double.infinity, 50), // عرض كامل
                ),
                child: Text(
                  'Зарегистрироваться',
                  style: TextStyle(fontSize: 18),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}