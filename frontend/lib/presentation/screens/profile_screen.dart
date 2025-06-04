import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/app_theme.dart';
import '../../providers/profile_provider.dart';
import '../../providers/auth_provider.dart';
import 'EditProfileScreen.dart';
import 'SettingsScreen.dart';
import 'CategoriesScreen.dart';
import 'GoalsScreen.dart';
import 'LimitsScreen.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  @override
  void initState() {
    super.initState();
    // Загружаем профиль при открытии экрана
    Future.microtask(() {
      Provider.of<ProfileProvider>(context, listen: false).fetchProfile();
    });
  }


  @override
  Widget build(BuildContext context) {
    final profileProvider = Provider.of<ProfileProvider>(context);
    final authProvider = Provider.of<AuthProvider>(context);
    
    return WillPopScope(
      // Обрабатываем нажатие кнопки "назад"
      onWillPop: () async {
        // Скрываем клавиатуру перед возвратом
        // Используем более надежный подход
        final currentFocus = FocusScope.of(context);
        if (!currentFocus.hasPrimaryFocus && currentFocus.focusedChild != null) {
          FocusManager.instance.primaryFocus?.unfocus();
        }
        
        // Передаем параметр, указывающий, что мы вернулись из профиля
        Navigator.pop(context, true);
        
        // Возвращаем false, чтобы предотвратить стандартное поведение WillPopScope
        return false;
      },
      child: Scaffold(
        appBar: AppBar(
          title: Text('Профиль'),
          leading: IconButton(
            icon: Icon(Icons.arrow_back),
            onPressed: () {
              // Скрываем клавиатуру перед возвратом
              // Используем более надежный подход
              final currentFocus = FocusScope.of(context);
              if (!currentFocus.hasPrimaryFocus && currentFocus.focusedChild != null) {
                FocusManager.instance.primaryFocus?.unfocus();
              }
              
              // Передаем параметр, указывающий, что мы вернулись из профиля
              Navigator.pop(context, true);
            },
          ),
          actions: [
            IconButton(
              icon: Icon(Icons.settings),
              onPressed: () {
                Navigator.pushNamed(context, '/settings');
              },
            ),
            IconButton(
              icon: Icon(Icons.logout),
              onPressed: () async {
                await authProvider.logout();
                if (!mounted) return;
                Navigator.pushReplacementNamed(context, '/login');
              },
            ),
          ],
        ),
        body: profileProvider.isLoading
            ? Center(child: CircularProgressIndicator())
            : profileProvider.error != null
                ? _buildErrorView(profileProvider.error!)
                : _buildProfileContent(profileProvider, authProvider),
      ),
    );
  }

  Widget _buildUserInfoSection(
      BuildContext context, ProfileProvider profileProvider) {
    final profile = profileProvider.profile;
    
    return Row(
      children: [
        CircleAvatar(
          radius: 30,
          backgroundColor: AppTheme.secondaryCardColor,
          backgroundImage: profile?.profileImage != null
              ? NetworkImage(profile!.profileImage!)
              : null,
          child: profile?.profileImage == null
              ? Icon(Icons.person, color: AppTheme.textColor)
              : null,
        ),
        SizedBox(width: 16),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              profile?.name ?? 'Пользователь',
              style: TextStyle(
                color: AppTheme.textColor,
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            Text(
              profile?.email ?? '',
              style: TextStyle(
                color: AppTheme.secondaryTextColor,
                fontSize: 14,
              ),
            ),
            IconButton(
              icon: Icon(Icons.edit, color: AppTheme.textColor),
              onPressed: () {
                Navigator.pushNamed(context, '/edit_profile');
              },
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildAchievementsSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Достижения',
          style: TextStyle(
            color: AppTheme.textColor,
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
        SizedBox(height: 8),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            _buildAchievementCard('Экономист', Icons.star),
            _buildAchievementCard('Накопитель', Icons.attach_money),
            _buildAchievementCard('Крутой', Icons.golf_course),
          ],
        ),
      ],
    );
  }

  Widget _buildAchievementCard(String title, IconData icon) {
    return Container(
      width: 100,
      height: 100,
      decoration: AppTheme.cardDecoration,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, color: AppTheme.textColor),
          SizedBox(height: 8),
          Text(
            title,
            style: TextStyle(
              color: AppTheme.textColor,
              fontSize: 14,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }

  Widget _buildFinancialGoalsSection(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              'Финансовые цели',
              style: TextStyle(
                color: AppTheme.textColor,
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            TextButton(
              onPressed: () {
                Navigator.pushNamed(context, '/goals');
              },
              child: Text('Все цели'),
            ),
          ],
        ),
        SizedBox(height: 8),
        _buildGoalItem('Накопить на машину', 0.75),
        SizedBox(height: 8),
        _buildGoalItem('Отпуск', 0.52),
      ],
    );
  }

  Widget _buildGoalItem(String goal, double progress) {
    return Container(
      decoration: AppTheme.cardDecoration,
      padding: EdgeInsets.all(16),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(
                  goal,
                  style: TextStyle(
                    color: AppTheme.textColor,
                    fontSize: 16,
                  ),
                ),
              ),
              Text(
                '${(progress * 100).toInt()}%',
                style: TextStyle(
                  color: AppTheme.textColor,
                  fontSize: 16,
                ),
              ),
            ],
          ),
          SizedBox(height: 8),
          LinearProgressIndicator(
            value: progress,
            backgroundColor: AppTheme.secondaryCardColor,
            valueColor: AlwaysStoppedAnimation<Color>(AppTheme.primaryColor),
          ),
        ],
      ),
    );
  }

  Widget _buildSettingsSection(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Настройки',
          style: TextStyle(
            color: AppTheme.textColor,
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
        SizedBox(height: 8),
        _buildSettingItem('Категории', Icons.category, context, () {
          Navigator.pushNamed(context, '/categories');
        }),
        _buildSettingItem('Цели', Icons.flag, context, () {
          Navigator.pushNamed(context, '/goals');
        }),
        _buildSettingItem('Лимиты', Icons.money_off, context, () {
          Navigator.pushNamed(context, '/limits');
        }),
        _buildSettingItem('Настройки', Icons.settings, context, () {
          Navigator.pushNamed(context, '/settings');
        }),
      ],
    );
  }

  Widget _buildSettingItem(String title, IconData icon, BuildContext context, VoidCallback onTap) {
    return Container(
      decoration: AppTheme.cardDecoration,
      margin: EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: Icon(icon, color: AppTheme.textColor),
        title: Text(
          title,
          style: TextStyle(color: AppTheme.textColor),
        ),
        trailing: Icon(Icons.arrow_forward_ios, color: AppTheme.textColor),
        onTap: onTap,
      ),
    );
  }

  Widget _buildErrorView(String error) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(
            'Ошибка загрузки профиля',
            style: TextStyle(color: Colors.red),
          ),
          SizedBox(height: 16),
          ElevatedButton(
            onPressed: () {
              Provider.of<ProfileProvider>(context, listen: false).fetchProfile();
            },
            child: Text('Повторить'),
          ),
        ],
      ),
    );
  }


  Widget _buildProfileContent(ProfileProvider profileProvider, AuthProvider authProvider) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Секция информации о пользователе
          _buildUserInfoSection(context, profileProvider),
          SizedBox(height: 24),

          // Секция достижений
          _buildAchievementsSection(),
          SizedBox(height: 24),

          // Секция финансовых целей с возможностью перехода на экран целей
          _buildFinancialGoalsSection(context),
          SizedBox(height: 24),

          // Секция настроек
          _buildSettingsSection(context),
          SizedBox(height: 24),

          // Секция PREMIUM подписки
          PremiumSubscriptionSection(), // 👈 Added here
        ],
      ),
    );

  }

}


//Subscription Section
class PremiumSubscriptionSection extends StatefulWidget {
  @override
  _PremiumSubscriptionSectionState createState() => _PremiumSubscriptionSectionState();
}

class _PremiumSubscriptionSectionState extends State<PremiumSubscriptionSection> {
  final TextEditingController _cardNumberController = TextEditingController();
  final TextEditingController _expiryDateController = TextEditingController();
  final TextEditingController _cvvController = TextEditingController();
  bool _isLoading = false;

  Future<void> _submitSubscription() async {
    setState(() => _isLoading = true);

    try {
      await Future.delayed(const Duration(seconds: 2));

      // Simulated POST request (replace with Dio later)
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Subscription successful (mocked)")),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text("Error: \$e")),
      );
    } finally {
      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(top: 24),
      color: AppTheme.cardColor,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Получить PREMIUM',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: AppTheme.textColor,
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _cardNumberController,
              decoration: const InputDecoration(labelText: 'Номер карты'),
              keyboardType: TextInputType.number,
            ),
            TextField(
              controller: _expiryDateController,
              decoration: const InputDecoration(labelText: 'Срок действия (MM/YY)'),
            ),
            TextField(
              controller: _cvvController,
              decoration: const InputDecoration(labelText: 'CVV'),
              obscureText: true,
              keyboardType: TextInputType.number,
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _isLoading ? null : _submitSubscription,
              child: _isLoading
                  ? const CircularProgressIndicator(color: Colors.white)
                  : const Text('Оплатить'),
            ),
          ],
        ),
      ),
    );
  }
}
