import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final authProvider = Provider.of<AuthProvider>(context);
    final userData = authProvider.authData;

    return Scaffold(
      appBar: AppBar(
        title: const Text('MoneyGuard'),
        actions: [
          IconButton(
            icon: const Icon(Icons.exit_to_app),
            onPressed: () async {
              await authProvider.logout();
              if (context.mounted) {
                Navigator.of(context).pushReplacementNamed('/login');
              }
            },
          ),
        ],
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('Добро пожаловать, ${userData?.name ?? "Пользователь"}!'),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: () {
                // Здесь будет переход к транзакциям
              },
              child: const Text('Мои транзакции'),
            ),
            ElevatedButton(
              onPressed: () {
                // Здесь будет переход к категориям
              },
              child: const Text('Категории'),
            ),
            ElevatedButton(
              onPressed: () {
                // Здесь будет переход к целям
              },
              child: const Text('Финансовые цели'),
            ),
            if (userData?.aiAccessEnabled == true)
              ElevatedButton(
                onPressed: () {
                  // Здесь будет переход к AI-ассистенту
                },
                child: const Text('AI-ассистент'),
              ),
          ],
        ),
      ),
    );
  }
} 