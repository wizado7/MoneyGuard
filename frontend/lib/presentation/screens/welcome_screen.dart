import 'dart:async';

import 'package:flutter/material.dart';
import '../widgets/circle_indicator.dart'; // استيراد CircleIndicator
import '../theme/app_theme.dart';

class WelcomeScreen extends StatefulWidget {
  const WelcomeScreen({super.key});

  @override
  _WelcomeScreenState createState() => _WelcomeScreenState();
}

class _WelcomeScreenState extends State<WelcomeScreen> {
  int currentStep = 0; // مؤشر لتتبع الجزء الحالي
  Timer? _timer;

  // قائمة بالنصوص التي سيتم عرضها لكل جزء
  final List<Map<String, String>> content = [
    {
      'title': 'Начните',
      'description': 'Управляйте своими финансами грамотно и четко',
    },
    {
      'title': 'Анализируйте',
      'description': 'Используйте ИИ-ассистента для экономии',
    },
    {
      'title': 'Достигайте',
      'description': 'Стройте удобные цели и задачи',
    },
  ];

  @override
  void initState() {
    super.initState();
    _startTimer();
  }

  void _startTimer() {
    _timer = Timer.periodic(Duration(seconds: 3), (timer) {
      if (currentStep < content.length - 1) {
        setState(() {
          currentStep++;
        });
      } else {
        _timer?.cancel();
      }
    });
  }

  void _navigateToNextStep() {
    if (currentStep < content.length - 1) {
      setState(() {
        currentStep++;
      });
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 32),
          child: Column(
            children: [
              Expanded(
                child: GestureDetector(
                  onTap: _navigateToNextStep,
                  child: Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        SizedBox(height: 40),
                        Text(
                          content[currentStep]['title']!,
                          style: Theme.of(context).textTheme.displayMedium,
                          textAlign: TextAlign.center,
                        ),
                        SizedBox(height: 16),
                        Text(
                          content[currentStep]['description']!,
                          style: TextStyle(
                            color: AppTheme.secondaryTextColor,
                            fontSize: 16,
                          ),
                          textAlign: TextAlign.center,
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: List.generate(
                      content.length,
                      (index) => CircleIndicator(isActive: index == currentStep),
                    ),
                  ),
                  SizedBox(height: 32),
                  ElevatedButton(
                    onPressed: () {
                      Navigator.pushReplacementNamed(context, '/login');
                    },
                    child: Text('Войти'),
                  ),
                  SizedBox(height: 16),
                  OutlinedButton(
                    onPressed: () {
                      Navigator.pushReplacementNamed(context, '/register');
                    },
                    style: OutlinedButton.styleFrom(
                      foregroundColor: AppTheme.primaryColor,
                      side: BorderSide(color: AppTheme.primaryColor),
                      minimumSize: Size(double.infinity, 56),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                    ),
                    child: Text('Зарегистрироваться'),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}