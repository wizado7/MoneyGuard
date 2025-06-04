import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/app_theme.dart';
import '../../providers/auth_provider.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  bool _notificationsEnabled = true;
  bool _darkModeEnabled = true;
  String _currency = '₽';
  final List<String> _currencies = ['₽', '\$', '€', '£'];

  @override
  Widget build(BuildContext context) {
    final authProvider = Provider.of<AuthProvider>(context);
    
    return Scaffold(
      appBar: AppBar(
        title: Text('Настройки'),
      ),
      body: ListView(
        padding: EdgeInsets.all(16),
        children: [


          SizedBox(height: 24),

          // Секция аккаунта
          _buildSectionHeader('Аккаунт'),
          _buildSettingItem(
            'Изменить пароль',
            Icons.lock,
            onTap: () {
              Navigator.pushNamed(context, '/change_password');
            },
          ),
          _buildSettingItem(
            'Удалить аккаунт',
            Icons.delete_forever,
            textColor: Colors.red,
            onTap: () {
              showDialog(
                context: context,
                builder: (context) => AlertDialog(
                  title: Text('Удалить аккаунт'),
                  content: Text(
                      'Вы уверены, что хотите удалить свой аккаунт? Это действие нельзя отменить.'),
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.pop(context),
                      child: Text('Отмена'),
                    ),
                    TextButton(
                      onPressed: () {
                        // Логика удаления аккаунта
                        Navigator.pop(context);
                      },
                      child: Text(
                        'Удалить',
                        style: TextStyle(color: Colors.red),
                      ),
                    ),
                  ],
                ),
              );
            },
          ),
          _buildSettingItem(
            'Выйти из аккаунта',
            Icons.logout,
            textColor: Colors.red,
            onTap: () async {
              showDialog(
                context: context,
                builder: (context) => AlertDialog(
                  title: Text('Выйти из аккаунта'),
                  content: Text('Вы уверены, что хотите выйти из аккаунта?'),
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.pop(context),
                      child: Text('Отмена'),
                    ),
                    TextButton(
                      onPressed: () async {
                        Navigator.pop(context);
                        await authProvider.logout();
                        if (!mounted) return;
                        Navigator.pushNamedAndRemoveUntil(
                          context,
                          '/login',
                          (route) => false,
                        );
                      },
                      child: Text(
                        'Выйти',
                        style: TextStyle(color: Colors.red),
                      ),
                    ),
                  ],
                ),
              );
            },
          ),

          SizedBox(height: 24),

          // Секция о приложении
          _buildSectionHeader('О приложении'),
          _buildSettingItem(
            'Версия',
            Icons.info_outline,
            trailing: Text(
              'v1.1a',
              style: TextStyle(color: AppTheme.secondaryTextColor),
            ),
          ),
          _buildSettingItem(
            'Политика конфиденциальности',
            Icons.privacy_tip_outlined,
            onTap: () {
              // Открыть политику конфиденциальности
            },
          ),
          _buildSettingItem(
            'Условия использования',
            Icons.description_outlined,
            onTap: () {
              // Открыть условия использования
            },
          ),
          _buildSettingItem(
            'Обратная связь',
            Icons.feedback_outlined,
            onTap: () {
              // Открыть форму обратной связи
            },
          ),
        ],
      ),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8.0),
      child: Text(
        title,
        style: TextStyle(
          color: AppTheme.primaryColor,
          fontSize: 18,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  Widget _buildSettingItem(
    String title,
    IconData icon, {
    Widget? trailing,
    VoidCallback? onTap,
    Color? textColor,
  }) {
    return Container(
      margin: EdgeInsets.only(bottom: 8),
      decoration: AppTheme.cardDecoration,
      child: ListTile(
        leading: Icon(icon, color: textColor ?? AppTheme.textColor),
        title: Text(
          title,
          style: TextStyle(color: textColor ?? AppTheme.textColor),
        ),
        trailing: trailing ?? (onTap != null ? Icon(Icons.arrow_forward_ios, color: AppTheme.textColor) : null),
        onTap: onTap,
      ),
    );
  }
} 