import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/app_theme.dart';
import '../../providers/goal_provider.dart';
import '../../models/goal.dart';
import 'package:intl/intl.dart';

class AddGoalScreen extends StatefulWidget {
  const AddGoalScreen({Key? key}) : super(key: key);

  @override
  _AddGoalScreenState createState() => _AddGoalScreenState();
}

class _AddGoalScreenState extends State<AddGoalScreen> {
  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _targetAmountController = TextEditingController();
  final TextEditingController _currentAmountController = TextEditingController();
  bool _isLoading = false;
  DateTime? _targetDate;
  String? _priority;
  final List<String> _priorities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  @override
  void dispose() {
    _nameController.dispose();
    _targetAmountController.dispose();
    _currentAmountController.dispose();
    super.dispose();
  }

  Future<void> _selectDate(BuildContext context) async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: DateTime.now(),
      firstDate: DateTime.now(),
      lastDate: DateTime(2100),
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: ColorScheme.dark(
              primary: AppTheme.primaryColor,
              onPrimary: Colors.black,
              surface: AppTheme.cardColor,
              onSurface: AppTheme.textColor,
            ),
            dialogBackgroundColor: AppTheme.backgroundColor,
          ),
          child: child!,
        );
      },
    );
    
    if (picked != null) {
      setState(() {
        _targetDate = picked;
      });
    }
  }

  Future<void> _saveGoal() async {
    // Проверяем, что все поля заполнены
    if (_nameController.text.isEmpty || _targetAmountController.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Пожалуйста, заполните все обязательные поля'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    // Парсим значения
    double targetAmount;
    double currentAmount = 0;

    try {
      targetAmount = double.parse(_targetAmountController.text.replaceAll(',', '.'));
      if (_currentAmountController.text.isNotEmpty) {
        currentAmount = double.parse(_currentAmountController.text.replaceAll(',', '.'));
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Некорректное значение суммы'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    // Проверяем, что целевая сумма положительная
    if (targetAmount <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Целевая сумма должна быть больше нуля'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    // Проверяем, что текущая сумма не больше целевой
    if (currentAmount > targetAmount) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Текущая сумма не может быть больше целевой'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    setState(() {
      _isLoading = true;
    });

    try {
      // Создаем новую цель
      final goal = Goal(
        name: _nameController.text,
        targetAmount: targetAmount,
        currentAmount: currentAmount,
        targetDate: _targetDate,
        priority: _priority,
        progress: currentAmount / targetAmount,
      );

      // Сохраняем цель
      await Provider.of<GoalProvider>(context, listen: false).addGoal(goal);

      // Возвращаемся на предыдущий экран
      Navigator.of(context).pop();

      // Показываем сообщение об успешном сохранении
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Цель успешно добавлена'),
          backgroundColor: Colors.green,
        ),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Ошибка при сохранении цели: ${e.toString()}'),
          backgroundColor: Colors.red,
        ),
      );
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Добавить цель'),
        leading: IconButton(
          icon: Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Название цели
            TextField(
              controller: _nameController,
              style: TextStyle(color: AppTheme.textColor),
              decoration: InputDecoration(
                labelText: 'Название',
                labelStyle: TextStyle(color: AppTheme.secondaryTextColor),
                filled: true,
                fillColor: AppTheme.secondaryCardColor,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8),
                  borderSide: BorderSide.none,
                ),
              ),
            ),
            SizedBox(height: 16),

            // Целевая сумма
            TextField(
              controller: _targetAmountController,
              keyboardType: TextInputType.numberWithOptions(decimal: true),
              style: TextStyle(color: AppTheme.textColor),
              decoration: InputDecoration(
                labelText: 'Сумма',
                labelStyle: TextStyle(color: AppTheme.secondaryTextColor),
                filled: true,
                fillColor: AppTheme.secondaryCardColor,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8),
                  borderSide: BorderSide.none,
                ),
              ),
            ),
            SizedBox(height: 16),

            // Текущая сумма (необязательно)
            TextField(
              controller: _currentAmountController,
              keyboardType: TextInputType.numberWithOptions(decimal: true),
              style: TextStyle(color: AppTheme.textColor),
              decoration: InputDecoration(
                labelText: 'Уже накоплено (необязательно)',
                labelStyle: TextStyle(color: AppTheme.secondaryTextColor),
                filled: true,
                fillColor: AppTheme.secondaryCardColor,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8),
                  borderSide: BorderSide.none,
                ),
              ),
            ),
            SizedBox(height: 16),

            // Дата достижения цели
            GestureDetector(
              onTap: () => _selectDate(context),
              child: Container(
                padding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                decoration: BoxDecoration(
                  color: AppTheme.secondaryCardColor,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      _targetDate == null
                          ? 'Дата достижения (необязательно)'
                          : DateFormat('yyyy-MM-dd').format(_targetDate!),
                      style: TextStyle(
                        color: _targetDate == null
                            ? AppTheme.secondaryTextColor
                            : AppTheme.textColor,
                      ),
                    ),
                    Icon(
                      Icons.calendar_today,
                      color: AppTheme.textColor,
                    ),
                  ],
                ),
              ),
            ),
            SizedBox(height: 16),

            // Приоритет
            _buildPriorityDropdown(),
            SizedBox(height: 32),

            // Кнопка сохранения
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _isLoading ? null : _saveGoal,
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.white,
                  foregroundColor: Colors.black,
                  padding: EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                ),
                child: _isLoading
                    ? SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.black,
                        ),
                      )
                    : Text('Добавить'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPriorityDropdown() {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      decoration: BoxDecoration(
        color: AppTheme.secondaryCardColor,
        borderRadius: BorderRadius.circular(8),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<String>(
          value: _priority,
          hint: Text(
            'Приоритет (необязательно)',
            style: TextStyle(color: AppTheme.secondaryTextColor),
          ),
          isExpanded: true,
          dropdownColor: AppTheme.cardColor,
          style: TextStyle(color: AppTheme.textColor),
          icon: Icon(Icons.arrow_drop_down, color: AppTheme.textColor),
          items: _priorities.map((String priority) {
            String displayText;
            Color priorityColor;
            
            switch (priority) {
              case 'LOW':
                displayText = 'Низкий';
                priorityColor = Colors.green;
                break;
              case 'MEDIUM':
                displayText = 'Средний';
                priorityColor = Colors.orange;
                break;
              case 'HIGH':
                displayText = 'Высокий';
                priorityColor = Colors.deepOrange;
                break;
              case 'CRITICAL':
                displayText = 'Критический';
                priorityColor = Colors.red;
                break;
              default:
                displayText = priority;
                priorityColor = AppTheme.textColor;
            }
            
            return DropdownMenuItem<String>(
              value: priority,
              child: Row(
                children: [
                  Container(
                    width: 12,
                    height: 12,
                    decoration: BoxDecoration(
                      color: priorityColor,
                      shape: BoxShape.circle,
                    ),
                  ),
                  SizedBox(width: 8),
                  Text(displayText),
                ],
              ),
            );
          }).toList(),
          onChanged: (String? value) {
            setState(() {
              _priority = value;
            });
          },
        ),
      ),
    );
  }
} 