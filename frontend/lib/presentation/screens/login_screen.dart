import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/app_theme.dart';
import '../../providers/auth_provider.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  _LoginScreenState createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  // المتحكمات لحقول الإدخال
  final TextEditingController _emailController = TextEditingController(); // البريد الإلكتروني
  final TextEditingController _passwordController = TextEditingController(); // كلمة المرور
  bool _obscurePassword = true;

  void _validateAndLogin(BuildContext context) async {
    // التحقق من أن جميع الحقول ممتلئة
    if (_emailController.text.isEmpty || _passwordController.text.isEmpty) {
      // عرض رسالة خطأ إذا كانت هناك حقول فارغة
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Пожалуйста, заполните все поля'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final success = await authProvider.login(
      _emailController.text,
      _passwordController.text,
    );

    if (success) {
      Navigator.pushReplacementNamed(context, '/home');
    } else {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(authProvider.error ?? 'Ошибка входа'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final authProvider = Provider.of<AuthProvider>(context);
    
    return Scaffold(
      appBar: AppBar(
        title: Text('Войти'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // حقل البريد الإلكتروني
            TextField(
              controller: _emailController,
              decoration: AppTheme.inputDecoration('Email', Icons.email),
              style: TextStyle(color: Colors.white),
              keyboardType: TextInputType.emailAddress,
              enabled: !authProvider.isLoading,
            ),

            SizedBox(height: 16),

            // حقل كلمة المرور
            TextField(
              controller: _passwordController,
              obscureText: _obscurePassword,
              decoration: InputDecoration(
                hintText: 'Пароль',
                prefixIcon: Icon(Icons.lock),
                suffixIcon: IconButton(
                  icon: Icon(
                    _obscurePassword ? Icons.visibility_off : Icons.visibility,
                  ),
                  onPressed: () {
                    setState(() {
                      _obscurePassword = !_obscurePassword;
                    });
                  },
                ),
                filled: true,
                fillColor: AppTheme.secondaryCardColor,
                hintStyle: TextStyle(color: AppTheme.secondaryTextColor),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8),
                  borderSide: BorderSide.none,
                ),
              ),
              style: TextStyle(color: Colors.white),
              enabled: !authProvider.isLoading,
            ),

            SizedBox(height: 8),

            // رابط إلى صفحة تسجيل جديد
            TextButton(
              onPressed: authProvider.isLoading
                  ? null
                  : () {
                      Navigator.pushNamed(context, '/register');
                    },
              child: Text(
                'Нет аккаунта? Зарегистрируйтесь',
                style: TextStyle(color: Colors.white),
              ),
            ),

            SizedBox(height: 30),

            // زر تسجيل الدخول
            authProvider.isLoading
                ? CircularProgressIndicator()
                : ElevatedButton(
                    onPressed: () => _validateAndLogin(context),
                    child: Text('Войти'),
                  ),
          ],
        ),
      ),
    );
  }

  @override
  void dispose() {
    // تحرير المتحكمات عند تدمير الـ Widget
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }
}