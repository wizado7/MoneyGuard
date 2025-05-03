import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../theme/app_theme.dart';
import '../../providers/transaction_provider.dart';
import '../../providers/category_provider.dart';
import '../../models/transaction.dart';
import '../../models/category.dart' as models;
import '../../providers/goal_provider.dart';
import '../../models/goal.dart';
import 'package:focus_detector/focus_detector.dart';
import '../../providers/auth_provider.dart';
import '../../utils/hex_color.dart';
import 'package:flutter/services.dart'; // Для TextInputFormatter

class AddTransactionScreen extends StatefulWidget {
  const AddTransactionScreen({super.key});

  @override
  State<AddTransactionScreen> createState() => _AddTransactionScreenState();
}

class _AddTransactionScreenState extends State<AddTransactionScreen> with WidgetsBindingObserver {
  final TextEditingController _amountController = TextEditingController();
  final TextEditingController _descriptionController = TextEditingController();
  final TextEditingController _amountToGoalController = TextEditingController();
  DateTime _selectedDate = DateTime.now();
  int? _selectedCategoryId;
  bool _isExpense = true;
  bool _isSubmitting = false;
  int? _selectedGoalId;
  final FocusNode _amountFocusNode = FocusNode();
  final FocusNode _descriptionFocusNode = FocusNode();
  bool _showAmountToGoalField = false;

  @override
  void initState() {
    super.initState();
    // Загружаем категории и цели при открытии экрана
    Future.microtask(() {
      Provider.of<CategoryProvider>(context, listen: false).fetchCategories();
      Provider.of<GoalProvider>(context, listen: false).fetchGoals();
    });
    
    // Регистрируем наблюдатель
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    _amountController.dispose();
    _descriptionController.dispose();
    _amountToGoalController.dispose();
    _amountFocusNode.dispose();
    _descriptionFocusNode.dispose();
    
    // Удаляем наблюдатель
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // Когда приложение возвращается на передний план
    if (state == AppLifecycleState.resumed) {
      // Убедимся, что клавиатура скрыта
      FocusManager.instance.primaryFocus?.unfocus();
    }
  }

  void _addTransaction() async {
    if (_amountController.text.isEmpty || _selectedCategoryId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Пожалуйста, заполните все обязательные поля')),
      );
      return;
    }

    if (_isSubmitting) return;
    
    setState(() {
      _isSubmitting = true;
    });

    try {
      double amount;
      double? amountToGoal;
      try {
        String amountText = _amountController.text.replaceAll(',', '.');
        amount = double.parse(amountText);

        if (_showAmountToGoalField && _amountToGoalController.text.isNotEmpty) {
          amountToGoal = double.parse(_amountToGoalController.text.replaceAll(',', '.'));
          if (amountToGoal! < 0) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text('Сумма для цели не может быть отрицательной'),
                backgroundColor: Colors.red,
              ),
            );
            setState(() {
              _isSubmitting = false;
            });
            return;
          }
          if (amountToGoal > amount.abs()) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text('Сумма для цели не может быть больше суммы дохода'),
                backgroundColor: Colors.red,
              ),
            );
            setState(() {
              _isSubmitting = false;
            });
            return;
          }
        }
      } catch (e) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Некорректная сумма'),
            backgroundColor: Colors.red,
          ),
        );
        setState(() {
          _isSubmitting = false;
        });
        return;
      }

      // Получаем строковый идентификатор пользователя
      final userId = Provider.of<AuthProvider>(context, listen: false).authData?.id.toString();

      final Transaction newTransaction = Transaction(
        amount: _isExpense ? -amount.abs() : amount.abs(),
        categoryId: _selectedCategoryId!,
        date: _selectedDate,
        description: _descriptionController.text,
        goalId: _selectedGoalId,
        amountContributedToGoal: amountToGoal,
        userId: userId ?? '',
        createdAt: DateTime.now(),
      );

      final success = await Provider.of<TransactionProvider>(context, listen: false)
          .addTransaction(newTransaction);

      if (success) {
        // Очищаем поля ввода вместо возврата на предыдущий экран
        if (mounted) {
          setState(() {
            _amountController.clear();
            _descriptionController.clear();
            _selectedDate = DateTime.now();
            _isSubmitting = false;
          });
          
          // Показываем сообщение об успешном добавлении
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('Транзакция успешно добавлена'),
              backgroundColor: Colors.green,
            ),
          );
        }
      } else {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Ошибка при сохранении транзакции')),
          );
          setState(() {
            _isSubmitting = false;
          });
        }
      }
    } catch (e) {
      print("AddTransactionScreen: Error saving transaction: $e");
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Ошибка: ${e.toString()}'),
            backgroundColor: Colors.red,
          ),
        );
        setState(() {
          _isSubmitting = false;
        });
      }
    }
  }

  Widget _buildGoalSelector(GoalProvider goalProvider) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Цель (необязательно)',
          style: TextStyle(
            color: AppTheme.textColor,
            fontSize: 16,
          ),
        ),
        SizedBox(height: 8),
        Container(
          padding: EdgeInsets.symmetric(horizontal: 12, vertical: 4),
          decoration: BoxDecoration(
            color: AppTheme.secondaryCardColor,
            borderRadius: BorderRadius.circular(8),
          ),
          child: DropdownButtonHideUnderline(
            child: DropdownButton<int>(
              value: _selectedGoalId,
              isExpanded: true,
              dropdownColor: AppTheme.secondaryCardColor,
              icon: Icon(Icons.arrow_drop_down, color: AppTheme.textColor),
              hint: Text('Выберите цель', style: TextStyle(color: AppTheme.textColor.withOpacity(0.5))),
              items: [
                DropdownMenuItem<int>(
                  value: null,
                  child: Text('Нет цели', style: TextStyle(color: AppTheme.textColor)),
                ),
                ...goalProvider.goals.map((goal) {
                  return DropdownMenuItem<int>(
                    value: goal.id,
                    child: Text(goal.name, style: TextStyle(color: AppTheme.textColor)),
                  );
                }).toList(),
              ],
              onChanged: (int? newGoalId) {
                setState(() {
                  _selectedGoalId = newGoalId;
                  _updateAmountToGoalVisibility();
                });
              },
            ),
          ),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    final transactionProvider = Provider.of<TransactionProvider>(context);
    final categoryProvider = Provider.of<CategoryProvider>(context);
    final goalProvider = Provider.of<GoalProvider>(context);
    
    // Фильтруем категории по типу (доходы/расходы)
    final categories = _isExpense 
        ? categoryProvider.expenseCategories
        : categoryProvider.incomeCategories;
  
    return Scaffold(
      appBar: AppBar(
        title: Text('Добавить транзакцию'),
        automaticallyImplyLeading: false,
      ),
      body: GestureDetector(
        onTap: () {
          FocusScope.of(context).unfocus();
        },
        child: Container(
          color: AppTheme.backgroundColor,
          child: SafeArea(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: SingleChildScrollView(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Переключатель типа транзакции
                    Container(
                      decoration: BoxDecoration(
                        color: AppTheme.cardColor,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Row(
                        children: [
                          Expanded(
                            child: GestureDetector(
                              onTap: () {
                                setState(() {
                                  _isExpense = true;
                                  _selectedCategoryId = null;
                                });
                              },
                              child: Container(
                                padding: EdgeInsets.symmetric(vertical: 12),
                                decoration: BoxDecoration(
                                  color: _isExpense ? AppTheme.primaryColor : Colors.transparent,
                                  borderRadius: BorderRadius.circular(8),
                                ),
                                child: Center(
                                  child: Text(
                                    'Расход',
                                    style: TextStyle(
                                      color: _isExpense ? Colors.black : AppTheme.textColor,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                ),
                              ),
                            ),
                          ),
                          Expanded(
                            child: GestureDetector(
                              onTap: () {
                                setState(() {
                                  _isExpense = false;
                                  _selectedCategoryId = null;
                                });
                              },
                              child: Container(
                                padding: EdgeInsets.symmetric(vertical: 12),
                                decoration: BoxDecoration(
                                  color: !_isExpense ? AppTheme.primaryColor : Colors.transparent,
                                  borderRadius: BorderRadius.circular(8),
                                ),
                                child: Center(
                                  child: Text(
                                    'Доход',
                                    style: TextStyle(
                                      color: !_isExpense ? Colors.black : AppTheme.textColor,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                ),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                    
                    SizedBox(height: 16),
                    
                    // Поле для суммы
                    Container(
                      margin: EdgeInsets.only(bottom: 16),
                      child: TextField(
                        controller: _amountController,
                        focusNode: _amountFocusNode,
                        keyboardType: TextInputType.numberWithOptions(decimal: true),
                        decoration: InputDecoration(
                          hintText: 'Сумма',
                          prefixIcon: Icon(Icons.attach_money, color: AppTheme.textColor),
                          filled: true,
                          fillColor: AppTheme.secondaryCardColor,
                          hintStyle: TextStyle(color: AppTheme.secondaryTextColor),
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(8),
                            borderSide: BorderSide.none,
                          ),
                        ),
                        style: TextStyle(color: AppTheme.textColor),
                      ),
                    ),
                    
                    // Категория
                    Text(
                      'Категория',
                      style: TextStyle(
                        color: AppTheme.textColor,
                        fontSize: 16,
                      ),
                    ),
                    SizedBox(height: 8),
                    Container(
                      height: 200,
                      child: _buildCategoryGrid(),
                    ),
                    
                    SizedBox(height: 16),
                    
                    // Дата
                    Text(
                      'Дата',
                      style: TextStyle(
                        color: AppTheme.textColor,
                        fontSize: 16,
                      ),
                    ),
                    SizedBox(height: 8),
                    GestureDetector(
                      onTap: () async {
                        final DateTime? picked = await showDatePicker(
                          context: context,
                          initialDate: _selectedDate,
                          firstDate: DateTime(2020),
                          lastDate: DateTime(2030),
                          builder: (BuildContext context, Widget? child) {
                            return Theme(
                              data: ThemeData.dark().copyWith(
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
                        if (picked != null && picked != _selectedDate) {
                          setState(() {
                            _selectedDate = picked;
                          });
                        }
                      },
                      child: Container(
                        padding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                        decoration: BoxDecoration(
                          color: AppTheme.secondaryCardColor,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Row(
                          children: [
                            Icon(Icons.calendar_today, color: AppTheme.textColor),
                            SizedBox(width: 8),
                            Text(
                              DateFormat('dd.MM.yyyy').format(_selectedDate),
                              style: TextStyle(color: AppTheme.textColor),
                            ),
                            Spacer(),
                            Icon(Icons.arrow_drop_down, color: AppTheme.textColor),
                          ],
                        ),
                      ),
                    ),
                    
                    SizedBox(height: 16),
                    
                    // Цель (только для доходов)
                    if (!_isExpense) ...[
                      _buildGoalSelector(goalProvider),
                      SizedBox(height: 16),
                    ],
                    
                    // Добавляем поле для суммы цели, если нужно
                    if (_showAmountToGoalField) ...[
                      Text(
                        'Сумма для цели',
                        style: TextStyle(
                          color: AppTheme.textColor,
                          fontSize: 16,
                        ),
                      ),
                      SizedBox(height: 8),
                      TextField(
                        controller: _amountToGoalController,
                        decoration: InputDecoration(
                          hintText: 'Введите сумму для цели',
                          filled: true,
                          fillColor: AppTheme.secondaryCardColor,
                          hintStyle: TextStyle(color: AppTheme.secondaryTextColor),
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(8),
                            borderSide: BorderSide.none,
                          ),
                        ),
                        style: TextStyle(color: AppTheme.textColor),
                        keyboardType: TextInputType.numberWithOptions(decimal: true),
                        inputFormatters: [
                          FilteringTextInputFormatter.allow(RegExp(r'^\d*[,.]?\d{0,2}')),
                        ],
                      ),
                      SizedBox(height: 16),
                    ],
                    
                    // Описание
                    Text(
                      'Описание (необязательно)',
                      style: TextStyle(
                        color: AppTheme.textColor,
                        fontSize: 16,
                      ),
                    ),
                    SizedBox(height: 8),
                    TextField(
                      controller: _descriptionController,
                      focusNode: _descriptionFocusNode,
                      decoration: InputDecoration(
                        hintText: 'Введите описание',
                        filled: true,
                        fillColor: AppTheme.secondaryCardColor,
                        hintStyle: TextStyle(color: AppTheme.secondaryTextColor),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(8),
                          borderSide: BorderSide.none,
                        ),
                      ),
                      style: TextStyle(color: AppTheme.textColor),
                      maxLines: 3,
                    ),
                    
                    SizedBox(height: 24),
                    
                    // Кнопка сохранения
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton(
                        onPressed: _isSubmitting || transactionProvider.isLoading ? null : _addTransaction,
                        child: _isSubmitting || transactionProvider.isLoading
                            ? SizedBox(
                                height: 20,
                                width: 20,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: Colors.black,
                                ),
                              )
                            : Text('Сохранить'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.white,
                          foregroundColor: Colors.black,
                          padding: EdgeInsets.symmetric(vertical: 16),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(8),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildCategoryGrid() {
    final categoryProvider = Provider.of<CategoryProvider>(context);
    
    // Фильтруем категории по типу (доходы/расходы)
    final categories = _isExpense 
        ? categoryProvider.categories.where((c) => !c.isIncome).toList()
        : categoryProvider.categories.where((c) => c.isIncome).toList();
    
    if (categories.isEmpty) {
      return Center(
        child: Text(
          'Нет доступных категорий',
          style: TextStyle(color: AppTheme.textColor),
        ),
      );
    }
    
    return GridView.builder(
      gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 4,
        childAspectRatio: 0.8,
        crossAxisSpacing: 8,
        mainAxisSpacing: 8,
      ),
      itemCount: categories.length,
      itemBuilder: (context, index) {
        final category = categories[index];
        final isSelected = _selectedCategoryId == category.id;
        
        return GestureDetector(
          onTap: () {
            setState(() {
              _selectedCategoryId = category.id;
            });
          },
          child: Container(
            decoration: BoxDecoration(
              color: isSelected ? category.color : AppTheme.secondaryCardColor,
              borderRadius: BorderRadius.circular(8),
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  category.icon,
                  color: isSelected ? Colors.black : category.color,
                  size: 32,
                ),
                SizedBox(height: 4),
                Text(
                  category.name,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: isSelected ? Colors.black : AppTheme.textColor,
                    fontSize: 12,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  void _updateAmountToGoalVisibility() {
    setState(() {
      // Показываем поле только если это доход и выбрана цель
      _showAmountToGoalField = !_isExpense && _selectedGoalId != null;
      
      // Если поле показывается и сумма уже введена, предлагаем всю сумму по умолчанию
      if (_showAmountToGoalField && _amountController.text.isNotEmpty) {
        _amountToGoalController.text = _amountController.text;
      } else if (!_showAmountToGoalField) {
        _amountToGoalController.clear();
      }
    });
  }

  void _onAmountChanged(String value) {
    if (_showAmountToGoalField) {
      setState(() {
        _amountToGoalController.text = value;
      });
    }
  }
} 