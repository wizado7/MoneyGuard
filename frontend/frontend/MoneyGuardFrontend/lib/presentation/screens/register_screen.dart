import 'package:flutter/material.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  _RegisterScreenState createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  // المتحكمات لحقول الإدخال
  final TextEditingController _firstNameController = TextEditingController(); // اسم الأول
  final TextEditingController _lastNameController = TextEditingController(); // اسم الأخير
  final TextEditingController _emailController = TextEditingController(); // البريد الإلكتروني
  final TextEditingController _passwordController = TextEditingController(); // كلمة المرور

  void _validateAndRegister(BuildContext context) {
    // التحقق من أن جميع الحقول ممتلئة
    if (_firstNameController.text.isEmpty ||
        _lastNameController.text.isEmpty ||
        _emailController.text.isEmpty ||
        _passwordController.text.isEmpty) {
      // عرض رسالة خطأ إذا كانت هناك حقول فارغة
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Пожалуйста, заполните все поля'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    // هنا يمكنك إضافة منطق التسجيل الحقيقي (مثل الاتصال بالخادم)
    print('تسجيل جديد: ${_emailController.text}');

    // العودة إلى شاشة تسجيل الدخول
    Navigator.pop(context);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Зарегистрироваться'),
        backgroundColor: Colors.black, // خلفية سوداء
      ),
      backgroundColor: Colors.black, // خلفية سوداء
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // حقل الاسم الأول
            TextField(
              controller: _firstNameController, // ربط المتحكم
              decoration: InputDecoration(
                hintText: 'Имя',
                icon: Icon(Icons.person),
                filled: true,
                fillColor: Colors.grey[800],
                hintStyle: TextStyle(color: Colors.grey[500]),
              ),
              style: TextStyle(color: Colors.white),
            ),

            SizedBox(height: 16),

            // حقل الاسم الأخير
            TextField(
              controller: _lastNameController, // ربط المتحكم
              decoration: InputDecoration(
                hintText: 'Фамилия',
                icon: Icon(Icons.person),
                filled: true,
                fillColor: Colors.grey[800],
                hintStyle: TextStyle(color: Colors.grey[500]),
              ),
              style: TextStyle(color: Colors.white),
            ),

            SizedBox(height: 16),

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

            // زر تسجيل جديد
            ElevatedButton(
              onPressed: () => _validateAndRegister(context), // التحقق والتسجيل
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
                'Зарегистрироваться',
                style: TextStyle(fontSize: 18),
              ),
            ),

            SizedBox(height: 16),

            // رابط إلى صفحة تسجيل الدخول
            TextButton(
              onPressed: () {
                Navigator.pop(context); // العودة إلى شاشة تسجيل الدخول
              },
              child: Text(
                'Уже есть аккаунт? Войти',
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
    _firstNameController.dispose();
    _lastNameController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }
}