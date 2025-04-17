import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../theme/app_theme.dart';
import '../../providers/goal_provider.dart';
import '../../models/goal.dart';

class EditGoalScreen extends StatefulWidget {
  final Goal goal;

  const EditGoalScreen({
    Key? key,
    required this.goal,
  }) : super(key: key);

  @override
  _EditGoalScreenState createState() => _EditGoalScreenState();
}

class _EditGoalScreenState extends State<EditGoalScreen> {
  late TextEditingController _nameController;
  late TextEditingController _targetAmountController;
  late TextEditingController _currentAmountController;
  late DateTime? _targetDate;
  late String? _priority;
  bool _isLoading = false;

  final List<String> _priorities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  @override
  void initState() {
    super.initState();
    
    _nameController = TextEditingController(text: widget.goal.name);
    _targetAmountController = TextEditingController(text: widget.goal.targetAmount.toString());
    _currentAmountController = TextEditingController(text: widget.goal.currentAmount.toString());
    _targetDate = widget.goal.targetDate;
    _priority = widget.goal.priority;
  }

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
      initialDate: _targetDate ?? DateTime.now(),
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
    
    if (picked != null && picked != _targetDate) {
      setState(() {
        _targetDate = picked;
      });
    }
  }

  Future<void> _updateGoal() async {
    if (_nameController.text.isEmpty || _targetAmountController.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Пожалуйста, заполните все обязательные поля'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    double targetAmount;
    double currentAmount = 0;

    try {
      targetAmount = double.parse(_targetAmountController.text.replaceAll(',', '.'));
      currentAmount = double.parse(_currentAmountController.text.replaceAll(',', '.'));
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Некорректное значение суммы'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    if (currentAmount < 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Текущая сумма не может быть отрицательной'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    if (targetAmount < currentAmount) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Целевая сумма не может быть меньше текущей'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    if (targetAmount <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Целевая сумма должна быть больше нуля'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    setState(() {
      _isLoading = true;
    });

    try {
      final updatedGoal = Goal(
        id: widget.goal.id,
        name: _nameController.text,
        targetAmount: targetAmount,
        currentAmount: currentAmount,
        targetDate: _targetDate,
        priority: _priority,
        progress: targetAmount > 0 ? currentAmount / targetAmount : 0,
      );

      final success = await Provider.of<GoalProvider>(context, listen: false).updateGoal(updatedGoal);

      if (success) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Цель успешно обновлена'),
            backgroundColor: Colors.green,
          ),
        );
        Navigator.of(context).pop();
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Ошибка при обновлении цели'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Ошибка: ${e.toString()}'),
          backgroundColor: Colors.red,
        ),
      );
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _deleteGoal() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: AppTheme.cardColor,
        title: Text(
          'Удалить цель?',
          style: TextStyle(color: AppTheme.textColor),
        ),
        content: Text(
          'Это действие нельзя отменить.',
          style: TextStyle(color: AppTheme.secondaryTextColor),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text(
              'Отмена',
              style: TextStyle(color: AppTheme.textColor),
            ),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: Text(
              'Удалить',
              style: TextStyle(color: Colors.red),
            ),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    setState(() {
      _isLoading = true;
    });

    try {
      await Provider.of<GoalProvider>(context, listen: false).deleteGoal(widget.goal.id!);

      Navigator.of(context).pop();

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Цель удалена'),
          backgroundColor: Colors.green,
        ),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Ошибка при удалении цели: ${e.toString()}'),
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
        title: Text('Редактировать цель'),
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

            // Текущая сумма
            TextField(
              controller: _currentAmountController,
              keyboardType: TextInputType.numberWithOptions(decimal: true),
              style: TextStyle(color: AppTheme.textColor),
              decoration: InputDecoration(
                labelText: 'Уже накоплено',
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

            // Кнопки действий
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _isLoading ? null : _updateGoal,
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
                    : Text('Сохранить'),
              ),
            ),
            SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _isLoading ? null : _deleteGoal,
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.white,
                  foregroundColor: Colors.red,
                  padding: EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                ),
                child: Text('Удалить'),
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