import 'package:flutter/material.dart';
import 'EditProfileScreen.dart';

class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(
          'Профиль', // عنوان الصفحة
          style: TextStyle(color: Colors.white), // تغيير لون النص إلى أبيض
        ),
        backgroundColor: Colors.black, // خلفية سوداء
        centerTitle: true, // مركز العنوان في الوسط
        leading: IconButton(
          icon: Icon(Icons.arrow_back, color: Colors.white),
          onPressed: () {
            Navigator.pop(context); // عودة إلى الشاشة السابقة
          },
        ),
      ),
      body: Container(
        color: Colors.black, // ضمان أن الخلفية سوداء
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // القسم الأول: معلومات المستخدم
              _buildUserInfoSection(context),

              SizedBox(height: 24),

              // القسم الثاني: دوائر الإنجازات
              _buildAchievementsSection(),

              SizedBox(height: 24),

              // القسم الثالث: الأهداف المالية
              _buildFinancialGoalsSection(),

              SizedBox(height: 24),

              // القسم الرابع: الإعدادات
              _buildSettingsSection(),
            ],
          ),
        ),
      ),
    );
  }

  // في ملف ProfileScreen
  Widget _buildUserInfoSection(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            CircleAvatar(
              radius: 30,
              backgroundColor: Colors.grey[800],
              child: Icon(Icons.person, color: Colors.white),
            ),
            SizedBox(width: 16),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Иванов Иван',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                IconButton(
                  icon: Icon(Icons.edit, color: Colors.white),
                  onPressed: () {
                    // تحويل المستخدم إلى صفحة تعديل الملف الشخصي
                    Navigator.push(
                      context,
                      MaterialPageRoute(builder: (context) => EditProfileScreen()),
                    );
                  },
                ),
              ],
            ),
          ],
        ),
      ],
    );
  }

  // قسم دوائر الإنجازات
  Widget _buildAchievementsSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Достижения',
          style: TextStyle(
            color: Colors.white,
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
        SizedBox(height: 8),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            _buildAchievementCard('Экономист', Icons.star),
            SizedBox(width: 2), // تقليل المسافة إلى 2 بكسل
            _buildAchievementCard('Накопитель', Icons.attach_money),
            SizedBox(width: 2), // تقليل المسافة إلى 2 بكسل
            _buildAchievementCard('Крутой', Icons.golf_course),
          ],
        ),
      ],
    );
  }

  // بناء كل دائرة إنجاز
  Widget _buildAchievementCard(String title, IconData icon) {
    return Container(
      width: 120,
      height: 100,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        color: Colors.grey[800],
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, color: Colors.white),
          SizedBox(height: 8),
          Text(
            title,
            style: TextStyle(
              color: Colors.white,
              fontSize: 16,
            ),
          ),
        ],
      ),
    );
  }

  // قسم الأهداف المالية
  Widget _buildFinancialGoalsSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Финансовые цели',
          style: TextStyle(
            color: Colors.white,
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
        SizedBox(height: 8),
        _buildGoalItem('Накопить на машину', 0.75), // 75%
        SizedBox(height: 8),
        _buildGoalItem('Отпуск', 0.52), // 52%
      ],
    );
  }

  // بناء كل هدف مالي مع ProgressBar
  Widget _buildGoalItem(String goal, double progress) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(8),
        color: Colors.grey[800],
      ),
      padding: EdgeInsets.symmetric(vertical: 8, horizontal: 16),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(
                  goal,
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 16,
                  ),
                ),
              ),
              Text(
                '${(progress * 100).toInt()}%',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                ),
              ),
            ],
          ),
          SizedBox(height: 8),
          LinearProgressIndicator(
            value: progress,
            backgroundColor: Colors.grey[800],
            valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
          ),
        ],
      ),
    );
  }

  // قسم الإعدادات
  Widget _buildSettingsSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Настройки',
          style: TextStyle(
            color: Colors.white,
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
        SizedBox(height: 8),
        ListTile(
          leading: Icon(Icons.settings, color: Colors.white),
          title: Text(
            'Подписки',
            style: TextStyle(color: Colors.white),
          ),
          trailing: Icon(Icons.arrow_forward_ios, color: Colors.white),
        ),
        ListTile(
          leading: Icon(Icons.security, color: Colors.white),
          title: Text(
            'Безопасность',
            style: TextStyle(color: Colors.white),
          ),
          trailing: Icon(Icons.arrow_forward_ios, color: Colors.white),
        ),
      ],
    );
  }
}