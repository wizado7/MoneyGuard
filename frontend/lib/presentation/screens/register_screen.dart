import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/app_theme.dart';
import '../../providers/auth_provider.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  _RegisterScreenState createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  bool _obscurePassword = true;

  void _validateAndRegister(BuildContext context) async {
    if (_nameController.text.isEmpty ||
        _emailController.text.isEmpty ||
        _passwordController.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Пожалуйста, заполните все поля'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final success = await authProvider.register(
      _emailController.text,
      _passwordController.text,
      _nameController.text,
    );

    if (success) {
      Navigator.pushReplacementNamed(context, '/home');
    } else {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(authProvider.error ?? 'Ошибка регистрации'),
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
        title: Text('Зарегистрироваться'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Создайте аккаунт',
              style: Theme.of(context).textTheme.displayMedium,
            ),
            SizedBox(height: 8),
            Text(
              'Для управления финансами',
              style: TextStyle(color: AppTheme.secondaryTextColor),
            ),
            SizedBox(height: 32),

            TextField(
              controller: _nameController,
              decoration: AppTheme.inputDecoration('Имя', Icons.person),
              style: TextStyle(color: Colors.white),
              enabled: !authProvider.isLoading,
            ),

            SizedBox(height: 16),

            TextField(
              controller: _emailController,
              decoration: AppTheme.inputDecoration('Email', Icons.email),
              style: TextStyle(color: Colors.white),
              keyboardType: TextInputType.emailAddress,
              enabled: !authProvider.isLoading,
            ),

            SizedBox(height: 16),

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

            SizedBox(height: 32),

            authProvider.isLoading
                ? Center(child: CircularProgressIndicator())
                : ElevatedButton(
                    onPressed: () => _validateAndRegister(context),
                    child: Text('Зарегистрироваться'),
                  ),

            SizedBox(height: 16),

            Center(
              child: TextButton(
                onPressed: authProvider.isLoading
                    ? null
                    : () {
                        Navigator.pushReplacementNamed(context, '/login');
                      },
                child: Text(
                  'Уже есть аккаунт? Войти',
                  style: TextStyle(color: Colors.white),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }
}