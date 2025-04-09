import 'package:flutter/material.dart';
import '../widgets/custom_input_field.dart'; // استيراد حقل الإدخال المخصص
import 'register_screen.dart'; // استيراد شاشة تسجيل جديد

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  _LoginScreenState createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  // المتحكمات لحقول الإدخال
  final TextEditingController _emailController = TextEditingController(); // البريد الإلكتروني
  final TextEditingController _passwordController = TextEditingController(); // كلمة المرور

  void _validateAndLogin(BuildContext context) {
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

    // هنا يمكنك إضافة منطق تسجيل الدخول الحقيقي (مثل الاتصال بالخادم)
    print('تسجيل الدخول: ${_emailController.text}');

    // الانتقال إلى الصفحة الرئيسية
    Navigator.pushReplacementNamed(context, '/home');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Войти'),
        backgroundColor: Colors.black, // خلفية سوداء
      ),
      backgroundColor: Colors.black, // خلفية سوداء
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // حقل البريد الإلكتروني
            TextField(
              controller: _emailController, // ربط المتحكم
              decoration: InputDecoration(
                hintText: 'Email',
                icon: Icon(Icons.email),
                filled: true,
                fillColor: Colors.grey[800],
                hintStyle: TextStyle(color: Colors.grey[500]),
              ),
              style: TextStyle(color: Colors.white),
            ),

            SizedBox(height: 16),

            // حقل كلمة المرور
            TextField(
              controller: _passwordController, // ربط المتحكم
              obscureText: true,
              decoration: InputDecoration(
                hintText: 'Пароль',
                icon: Icon(Icons.lock),
                filled: true,
                fillColor: Colors.grey[800],
                hintStyle: TextStyle(color: Colors.grey[500]),
              ),
              style: TextStyle(color: Colors.white),
            ),

            SizedBox(height: 30),

            // زر تسجيل الدخول
            ElevatedButton(
              onPressed: () => _validateAndLogin(context), // التحقق والدخول
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.white, // الخلفية البيضاء
                foregroundColor: Colors.black, // النص الأسود
                padding: EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
                minimumSize: Size(double.infinity, 50),
              ),
              child: Text(
                'Войти',
                style: TextStyle(fontSize: 18),
              ),
            ),

            SizedBox(height: 16),

            // رابط إلى صفحة تسجيل جديد
            TextButton(
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (context) => RegisterScreen()),
                );
              },
              child: Text(
                'Нет аккаунта? Зарегистрируйтесь',
                style: TextStyle(color: Colors.white),
              ),
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