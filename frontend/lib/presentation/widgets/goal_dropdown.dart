import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/goal.dart';
import '../../providers/goal_provider.dart';
import '../theme/app_theme.dart';

class GoalDropdown extends StatelessWidget {
  final Goal? selectedGoal;
  final Function(Goal?) onChanged;
  final bool allowNull;

  const GoalDropdown({
    Key? key,
    this.selectedGoal,
    required this.onChanged,
    this.allowNull = true,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final goalProvider = Provider.of<GoalProvider>(context);
    final goals = goalProvider.goals;

    if (goals.isEmpty) {
      return Text(
        'Нет доступных целей',
        style: TextStyle(color: AppTheme.secondaryTextColor),
      );
    }

    // Создаем список элементов для выпадающего списка
    List<DropdownMenuItem<Goal?>> items = [];
    
    // Добавляем опцию "Нет цели", если allowNull = true
    if (allowNull) {
      items.add(
        DropdownMenuItem<Goal?>(
          value: null,
          child: Text(
            'Нет цели',
            style: TextStyle(color: AppTheme.textColor),
          ),
        ),
      );
    }

    // Добавляем все доступные цели
    items.addAll(
      goals.map((goal) {
        return DropdownMenuItem<Goal?>(
          value: goal,
          child: Text(
            goal.name,
            style: TextStyle(color: AppTheme.textColor),
          ),
        );
      }).toList(),
    );

    return DropdownButtonFormField<Goal?>(
      value: selectedGoal,
      decoration: InputDecoration(
        filled: true,
        fillColor: AppTheme.secondaryCardColor,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: BorderSide.none,
        ),
        contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      ),
      style: TextStyle(color: AppTheme.textColor),
      dropdownColor: AppTheme.cardColor,
      items: items,
      onChanged: (value) {
        onChanged(value);
      },
    );
  }
} 