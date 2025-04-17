import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/category.dart' as models;
import '../../providers/category_provider.dart';
import '../theme/app_theme.dart';

class CategoryDropdown extends StatelessWidget {
  final models.Category? selectedCategory;
  final Function(models.Category) onChanged;
  final bool isIncome;

  const CategoryDropdown({
    Key? key,
    required this.selectedCategory,
    required this.onChanged,
    required this.isIncome,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final categoryProvider = Provider.of<CategoryProvider>(context);
    final categories = categoryProvider.categories
        .where((cat) => cat.isIncome == isIncome)
        .toList();

    if (categories.isEmpty) {
      return Text(
        'Нет доступных категорий',
        style: TextStyle(color: AppTheme.secondaryTextColor),
      );
    }

    return DropdownButtonFormField<models.Category>(
      value: selectedCategory,
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
      items: categories.map((category) {
        return DropdownMenuItem<models.Category>(
          value: category,
          child: Row(
            children: [
              Icon(category.icon, color: category.color, size: 20),
              SizedBox(width: 8),
              Text(
                category.name,
                style: TextStyle(color: AppTheme.textColor),
              ),
            ],
          ),
        );
      }).toList(),
      onChanged: (value) {
        if (value != null) {
          onChanged(value);
        }
      },
    );
  }
} 