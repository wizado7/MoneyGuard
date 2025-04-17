import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/app_theme.dart';
import '../../providers/limit_provider.dart';
import '../../providers/category_provider.dart';
import '../../models/limit.dart';
import '../../models/category.dart';
import '../../models/category.dart' as models;

class LimitsScreen extends StatefulWidget {
  const LimitsScreen({super.key});

  @override
  State<LimitsScreen> createState() => _LimitsScreenState();
}

class _LimitsScreenState extends State<LimitsScreen> {
  final TextEditingController _amountController = TextEditingController();
  int? _selectedCategoryId;
  String _selectedPeriod = 'Месяц';
  final List<String> _periods = ['День', 'Неделя', 'Месяц', 'Год'];

  @override
  void initState() {
    super.initState();
    // Загружаем лимиты и категории при открытии экрана
    Future.microtask(() {
      Provider.of<LimitProvider>(context, listen: false).fetchLimits();
      Provider.of<CategoryProvider>(context, listen: false).fetchCategories();
    });
  }

  void _showAddLimitDialog() {
    _amountController.clear();
    _selectedCategoryId = null;
    _selectedPeriod = 'Месяц';

    showDialog(
      context: context,
      builder: (context) {
        final categoryProvider = Provider.of<CategoryProvider>(context);
        
        return AlertDialog(
          title: Text('Добавить лимит'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: _amountController,
                  decoration: AppTheme.inputDecoration('Сумма', Icons.attach_money),
                  style: TextStyle(color: AppTheme.textColor),
                  keyboardType: TextInputType.numberWithOptions(decimal: true),
                ),
                SizedBox(height: 16),
                Container(
                  padding: EdgeInsets.symmetric(horizontal: 16),
                  decoration: AppTheme.cardDecoration,
                  child: DropdownButton<String>(
                    value: _selectedPeriod,
                    isExpanded: true,
                    dropdownColor: AppTheme.cardColor,
                    style: TextStyle(color: AppTheme.textColor),
                    underline: SizedBox(),
                    onChanged: (String? newValue) {
                      if (newValue != null) {
                        setState(() {
                          _selectedPeriod = newValue;
                        });
                        Navigator.pop(context);
                        _showAddLimitDialog();
                      }
                    },
                    items: _periods.map<DropdownMenuItem<String>>((String value) {
                      return DropdownMenuItem<String>(
                        value: value,
                        child: Text(value),
                      );
                    }).toList(),
                  ),
                ),
                SizedBox(height: 16),
                Text(
                  'Выберите категорию',
                  style: TextStyle(color: AppTheme.textColor),
                ),
                SizedBox(height: 8),
                categoryProvider.isLoading
                    ? Center(child: CircularProgressIndicator())
                    : categoryProvider.error != null
                        ? Center(
                            child: Text(
                              'Ошибка загрузки категорий',
                              style: TextStyle(color: Colors.red),
                            ),
                          )
                        : categoryProvider.expenseCategories.isEmpty
                            ? Center(
                                child: Text(
                                  'Нет доступных категорий',
                                  style: TextStyle(color: AppTheme.textColor),
                                ),
                              )
                            : Container(
                                height: 200,
                                child: GridView.builder(
                                  gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                                    crossAxisCount: 3,
                                    childAspectRatio: 1,
                                    crossAxisSpacing: 8,
                                    mainAxisSpacing: 8,
                                  ),
                                  itemCount: categoryProvider.expenseCategories.length,
                                  itemBuilder: (context, index) {
                                    final category = categoryProvider.expenseCategories[index];
                                    final isSelected = _selectedCategoryId == category.id;
                                    
                                    return GestureDetector(
                                      onTap: () {
                                        setState(() {
                                          _selectedCategoryId = category.id;
                                        });
                                        Navigator.pop(context);
                                        _showAddLimitDialog();
                                      },
                                      child: Container(
                                        decoration: BoxDecoration(
                                          color: isSelected
                                              ? AppTheme.primaryColor
                                              : AppTheme.cardColor,
                                          borderRadius: BorderRadius.circular(8),
                                        ),
                                        child: Column(
                                          mainAxisAlignment: MainAxisAlignment.center,
                                          children: [
                                            Icon(
                                              category.icon,
                                              color: isSelected
                                                  ? Colors.black
                                                  : category.color,
                                              size: 32,
                                            ),
                                            SizedBox(height: 8),
                                            Text(
                                              category.name,
                                              style: TextStyle(
                                                color: isSelected
                                                    ? Colors.black
                                                    : AppTheme.textColor,
                                                fontSize: 12,
                                              ),
                                              textAlign: TextAlign.center,
                                              overflow: TextOverflow.ellipsis,
                                            ),
                                          ],
                                        ),
                                      ),
                                    );
                                  },
                                ),
                              ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
              },
              child: Text('Отмена'),
            ),
            ElevatedButton(
              onPressed: () {
                if (_amountController.text.isNotEmpty && _selectedCategoryId != null) {
                  _addLimit();
                  Navigator.of(context).pop();
                } else {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text('Пожалуйста, заполните все поля'),
                      backgroundColor: Colors.red,
                    ),
                  );
                }
              },
              child: Text('Добавить'),
            ),
          ],
        );
      },
    );
  }

  Future<void> _addLimit() async {
    try {
      final amount = double.parse(_amountController.text);
      
      final categoryProvider = Provider.of<CategoryProvider>(context);
      final category = categoryProvider.categories.firstWhere(
        (c) => c.id == _selectedCategoryId,
        orElse: () => throw Exception('Категория не найдена'),
      );
      
      final limit = Limit(
        id: 0,
        name: 'Новый лимит',
        amount: amount,
        period: _selectedPeriod,
        startDate: DateTime.now(),
        endDate: DateTime.now().add(Duration(days: 30)),
        categoryId: _selectedCategoryId!,
        categoryName: category.name,
        currentUsage: 0.0,
      );
      
      final limitProvider = Provider.of<LimitProvider>(context, listen: false);
      final success = await limitProvider.addLimit(limit);
      
      if (success && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Лимит успешно добавлен')),
        );
      } else if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Ошибка добавления лимита')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Ошибка: $e')),
        );
      }
    }
  }

  void _showEditLimitDialog(Limit limit) {
    _amountController.text = limit.amount.toString();
    _selectedCategoryId = limit.categoryId;
    _selectedPeriod = limit.period;

    showDialog(
      context: context,
      builder: (context) {
        final categoryProvider = Provider.of<CategoryProvider>(context);
        
        return AlertDialog(
          title: Text('Редактировать лимит'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: _amountController,
                  decoration: AppTheme.inputDecoration('Сумма', Icons.attach_money),
                  style: TextStyle(color: AppTheme.textColor),
                  keyboardType: TextInputType.numberWithOptions(decimal: true),
                ),
                SizedBox(height: 16),
                Container(
                  padding: EdgeInsets.symmetric(horizontal: 16),
                  decoration: AppTheme.cardDecoration,
                  child: DropdownButton<String>(
                    value: _selectedPeriod,
                    isExpanded: true,
                    dropdownColor: AppTheme.cardColor,
                    style: TextStyle(color: AppTheme.textColor),
                    underline: SizedBox(),
                    onChanged: (String? newValue) {
                      if (newValue != null) {
                        setState(() {
                          _selectedPeriod = newValue;
                        });
                        Navigator.pop(context);
                        _showEditLimitDialog(limit);
                      }
                    },
                    items: _periods.map<DropdownMenuItem<String>>((String value) {
                      return DropdownMenuItem<String>(
                        value: value,
                        child: Text(value),
                      );
                    }).toList(),
                  ),
                ),
                SizedBox(height: 16),
                Text(
                  'Выберите категорию',
                  style: TextStyle(color: AppTheme.textColor),
                ),
                SizedBox(height: 8),
                categoryProvider.isLoading
                    ? Center(child: CircularProgressIndicator())
                    : categoryProvider.error != null
                        ? Center(
                            child: Text(
                              'Ошибка загрузки категорий',
                              style: TextStyle(color: Colors.red),
                            ),
                          )
                        : categoryProvider.expenseCategories.isEmpty
                            ? Center(
                                child: Text(
                                  'Нет доступных категорий',
                                  style: TextStyle(color: AppTheme.textColor),
                                ),
                              )
                            : Container(
                                height: 200,
                                child: GridView.builder(
                                  gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                                    crossAxisCount: 3,
                                    childAspectRatio: 1,
                                    crossAxisSpacing: 8,
                                    mainAxisSpacing: 8,
                                  ),
                                  itemCount: categoryProvider.expenseCategories.length,
                                  itemBuilder: (context, index) {
                                    final category = categoryProvider.expenseCategories[index];
                                    final isSelected = _selectedCategoryId == category.id;
                                    
                                    return GestureDetector(
                                      onTap: () {
                                        setState(() {
                                          _selectedCategoryId = category.id;
                                        });
                                        Navigator.pop(context);
                                        _showEditLimitDialog(limit);
                                      },
                                      child: Container(
                                        decoration: BoxDecoration(
                                          color: isSelected
                                              ? AppTheme.primaryColor
                                              : AppTheme.cardColor,
                                          borderRadius: BorderRadius.circular(8),
                                        ),
                                        child: Column(
                                          mainAxisAlignment: MainAxisAlignment.center,
                                          children: [
                                            Icon(
                                              category.icon,
                                              color: isSelected
                                                  ? Colors.black
                                                  : category.color,
                                              size: 32,
                                            ),
                                            SizedBox(height: 8),
                                            Text(
                                              category.name,
                                              style: TextStyle(
                                                color: isSelected
                                                    ? Colors.black
                                                    : AppTheme.textColor,
                                                fontSize: 12,
                                              ),
                                              textAlign: TextAlign.center,
                                              overflow: TextOverflow.ellipsis,
                                            ),
                                          ],
                                        ),
                                      ),
                                    );
                                  },
                                ),
                              ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
              },
              child: Text('Отмена'),
            ),
            ElevatedButton(
              onPressed: () {
                if (_amountController.text.isNotEmpty && _selectedCategoryId != null) {
                  _updateLimit(limit);
                  Navigator.of(context).pop();
                } else {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text('Пожалуйста, заполните все поля'),
                      backgroundColor: Colors.red,
                    ),
                  );
                }
              },
              child: Text('Сохранить'),
            ),
          ],
        );
      },
    );
  }

  Future<void> _updateLimit(Limit limit) async {
    try {
      final amount = double.parse(_amountController.text);
      
      final categoryProvider = Provider.of<CategoryProvider>(context);
      final category = categoryProvider.categories.firstWhere(
        (c) => c.id == _selectedCategoryId,
        orElse: () => throw Exception('Категория не найдена'),
      );
      
      final updatedLimit = Limit(
        id: limit.id,
        name: limit.name,
        amount: amount,
        period: _selectedPeriod,
        startDate: limit.startDate,
        endDate: limit.endDate,
        categoryId: _selectedCategoryId!,
        categoryName: category.name,
        currentUsage: limit.currentUsage,
      );
      
      final limitProvider = Provider.of<LimitProvider>(context, listen: false);
      final success = await limitProvider.updateLimit(updatedLimit);
      
      if (success && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Лимит успешно обновлен')),
        );
      } else if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Ошибка обновления лимита')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Ошибка: $e')),
        );
      }
    }
  }

  void _deleteLimit(Limit limit) async {
    final limitProvider = Provider.of<LimitProvider>(context, listen: false);
    final success = await limitProvider.deleteLimit(limit.id!);
    
    if (success) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Лимит удален'),
          backgroundColor: Colors.green,
        ),
      );
    } else {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(limitProvider.error ?? 'Ошибка удаления лимита'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  Color _getLimitColor(double percentageUsed) {
    if (percentageUsed < 0.5) return Colors.green;
    if (percentageUsed < 0.8) return Colors.orange;
    return Colors.red;
  }

  @override
  Widget build(BuildContext context) {
    final limitProvider = Provider.of<LimitProvider>(context);
    final categoryProvider = Provider.of<CategoryProvider>(context);
    
    return Scaffold(
      appBar: AppBar(
        title: Text('Лимиты расходов'),
      ),
      body: limitProvider.isLoading || categoryProvider.isLoading
          ? Center(child: CircularProgressIndicator())
          : limitProvider.error != null || categoryProvider.error != null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        'Ошибка загрузки данных',
                        style: TextStyle(color: Colors.red),
                      ),
                      SizedBox(height: 16),
                      ElevatedButton(
                        onPressed: () {
                          limitProvider.fetchLimits();
                          categoryProvider.fetchCategories();
                        },
                        child: Text('Повторить'),
                      ),
                    ],
                  ),
                )
              : limitProvider.limits.isEmpty
                  ? Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            Icons.money_off,
                            size: 64,
                            color: AppTheme.secondaryTextColor,
                          ),
                          SizedBox(height: 16),
                          Text(
                            'У вас пока нет лимитов расходов',
                            style: TextStyle(
                              color: AppTheme.secondaryTextColor,
                              fontSize: 16,
                            ),
                            textAlign: TextAlign.center,
                          ),
                          SizedBox(height: 16),
                          ElevatedButton(
                            onPressed: _showAddLimitDialog,
                            child: Text('Добавить лимит'),
                          ),
                        ],
                      ),
                    )
                  : ListView.builder(
                      padding: EdgeInsets.all(16),
                      itemCount: limitProvider.limits.length,
                      itemBuilder: (context, index) {
                        final limit = limitProvider.limits[index];
                        final category = categoryProvider.categories.firstWhere(
                          (c) => c.id == limit.categoryId,
                          orElse: () => models.Category(
                            id: limit.categoryId,
                            name: limit.categoryName,
                            iconName: 'category',
                            colorHex: '#9E9E9E',
                            isIncome: false,
                          ),
                        );
                        
                        return Card(
                          margin: EdgeInsets.only(bottom: 16),
                          color: AppTheme.cardColor,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(16),
                          ),
                          child: Padding(
                            padding: EdgeInsets.all(16),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                  children: [
                                    Row(
                                      children: [
                                        Container(
                                          padding: EdgeInsets.all(8),
                                          decoration: BoxDecoration(
                                            color: category.color.withOpacity(0.2),
                                            borderRadius: BorderRadius.circular(8),
                                          ),
                                          child: Icon(
                                            category.icon,
                                            color: category.color,
                                          ),
                                        ),
                                        SizedBox(width: 16),
                                        Text(
                                          category.name,
                                          style: TextStyle(
                                            color: AppTheme.textColor,
                                            fontSize: 18,
                                            fontWeight: FontWeight.bold,
                                          ),
                                        ),
                                      ],
                                    ),
                                    Row(
                                      children: [
                                        IconButton(
                                          icon: Icon(Icons.edit, color: AppTheme.textColor),
                                          onPressed: () => _showEditLimitDialog(limit),
                                        ),
                                        IconButton(
                                          icon: Icon(Icons.delete, color: Colors.red),
                                          onPressed: () {
                                            showDialog(
                                              context: context,
                                              builder: (context) => AlertDialog(
                                                title: Text('Удалить лимит'),
                                                content: Text(
                                                    'Вы уверены, что хотите удалить лимит для категории "${category.name}"?'),
                                                actions: [
                                                  TextButton(
                                                    onPressed: () => Navigator.pop(context),
                                                    child: Text('Отмена'),
                                                  ),
                                                  TextButton(
                                                    onPressed: () {
                                                      _deleteLimit(limit);
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
                                      ],
                                    ),
                                  ],
                                ),
                                SizedBox(height: 16),
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                  children: [
                                    Text(
                                      'Лимит:',
                                      style: TextStyle(color: AppTheme.textColor),
                                    ),
                                    Text(
                                      '${limit.amount.toStringAsFixed(0)} ₽ / ${limit.period.toLowerCase()}',
                                      style: TextStyle(
                                        color: AppTheme.textColor,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                  ],
                                ),
                                SizedBox(height: 8),
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                  children: [
                                    Text(
                                      'Использовано:',
                                      style: TextStyle(color: AppTheme.textColor),
                                    ),
                                    Text(
                                      '${limit.currentUsage.toStringAsFixed(0)} ₽',
                                      style: TextStyle(
                                        color: AppTheme.textColor,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                  ],
                                ),
                                SizedBox(height: 8),
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                  children: [
                                    Text(
                                      'Осталось:',
                                      style: TextStyle(color: AppTheme.textColor),
                                    ),
                                    Text(
                                      '${limit.remaining.toStringAsFixed(0)} ₽',
                                      style: TextStyle(
                                        color: _getLimitColor(limit.percentageUsed),
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                  ],
                                ),
                                SizedBox(height: 16),
                                LinearProgressIndicator(
                                  value: limit.percentageUsed,
                                  backgroundColor: AppTheme.secondaryCardColor,
                                  valueColor: AlwaysStoppedAnimation<Color>(
                                    _getLimitColor(limit.percentageUsed),
                                  ),
                                ),
                                SizedBox(height: 8),
                                Text(
                                  'Использовано: ${(limit.percentageUsed * 100).toInt()}%',
                                  style: TextStyle(
                                    color: _getLimitColor(limit.percentageUsed),
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
      floatingActionButton: FloatingActionButton(
        onPressed: _showAddLimitDialog,
        backgroundColor: AppTheme.primaryColor,
        foregroundColor: Colors.black,
        child: Icon(Icons.add),
      ),
    );
  }

  @override
  void dispose() {
    _amountController.dispose();
    super.dispose();
  }
} 