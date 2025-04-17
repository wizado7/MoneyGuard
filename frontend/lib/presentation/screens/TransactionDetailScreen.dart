import 'package:flutter/material.dart';
import 'package:flutter/services.dart'; // Для TextInputFormatter
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import 'package:collection/collection.dart'; // Импортируем пакет collection для firstWhereOrNull
import '../theme/app_theme.dart';
import '../../providers/transaction_provider.dart';
import '../../providers/category_provider.dart';
import '../../models/transaction.dart';
import '../../models/category.dart' as models;
import '../../models/goal.dart';
import '../../providers/goal_provider.dart';
import '../widgets/category_dropdown.dart'; // Если используется для редактирования
import '../widgets/goal_dropdown.dart'; // Если используется для редактирования

class TransactionDetailScreen extends StatefulWidget {
  final Transaction transaction;

  const TransactionDetailScreen({
    Key? key,
    required this.transaction,
  }) : super(key: key);

  @override
  _TransactionDetailScreenState createState() => _TransactionDetailScreenState();
}

class _TransactionDetailScreenState extends State<TransactionDetailScreen> {
  late TextEditingController _amountController;
  late TextEditingController _descriptionController;
  late TextEditingController _amountToGoalController;
  late DateTime _selectedDate;
  int? _selectedCategoryId;
  models.Category? _selectedCategoryObject;
  int? _selectedGoalId;
  bool _isExpense = true;
  bool _isEditing = false;
  bool _isLoading = false;
  bool _showAmountToGoalField = false;

  @override
  void initState() {
    super.initState();
    
    _amountController = TextEditingController(
      text: widget.transaction.amount.abs().toStringAsFixed(2)
    );
    _descriptionController = TextEditingController(
      text: widget.transaction.description ?? ''
    );
    _selectedDate = widget.transaction.date;
    _isExpense = !widget.transaction.isIncome;
    _selectedCategoryId = widget.transaction.categoryId;
    _selectedGoalId = widget.transaction.goalId;
    
    // Инициализируем контроллер для суммы цели
    _amountToGoalController = TextEditingController();
    if (widget.transaction.amountToGoal != null) {
      _amountToGoalController.text = widget.transaction.amountToGoal!.toStringAsFixed(2);
    }
    
    // Обновляем видимость поля суммы для цели
    _updateAmountToGoalVisibility(initialize: true);

    Future.microtask(() {
      final categoryProvider = Provider.of<CategoryProvider>(context, listen: false);
      final goalProvider = Provider.of<GoalProvider>(context, listen: false);
      categoryProvider.fetchCategories();
      goalProvider.fetchGoals();

      if (_selectedCategoryId != null && categoryProvider.categories.isNotEmpty) {
         try {
           _selectedCategoryObject = categoryProvider.categories.firstWhere(
                 (cat) => cat.id == _selectedCategoryId,
           );
         } catch (e) {
           print("Не удалось найти категорию с ID: $_selectedCategoryId");
           _selectedCategoryObject = null;
           _selectedCategoryId = null;
         }
      }
      _updateAmountToGoalVisibility();
      if (mounted) {
        setState(() {});
      }
    });
  }

  @override
  void dispose() {
    _amountController.dispose();
    _descriptionController.dispose();
    _amountToGoalController.dispose();
    super.dispose();
  }

  void _updateAmountToGoalVisibility({bool initialize = false}) {
    final shouldShow = !_isExpense && _selectedGoalId != null;

    if (initialize && widget.transaction.isIncome && widget.transaction.goalId != null) {
       final initialAmount = widget.transaction.amountToGoal ?? widget.transaction.amount.abs();
       _amountToGoalController.text = initialAmount.toStringAsFixed(2);
    } else if (shouldShow && _amountController.text.isNotEmpty) {
       _amountToGoalController.text = _amountController.text;
    } else if (!shouldShow) {
       _amountToGoalController.clear();
    }

    if (_showAmountToGoalField != shouldShow) {
       setState(() {
         _showAmountToGoalField = shouldShow;
       });
    } else if (initialize) {
       _showAmountToGoalField = shouldShow;
    }
  }

   void _onAmountChanged(String value) {
      if (_showAmountToGoalField) {
         setState(() {
            _amountToGoalController.text = value;
         });
      }
   }

  Future<void> _selectDate(BuildContext context) async {
    if (!_isEditing) return;
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: _selectedDate,
      firstDate: DateTime(2000),
      lastDate: DateTime(2101),
    );
    if (picked != null && picked != _selectedDate) {
      setState(() {
        _selectedDate = picked;
      });
    }
  }

  Future<void> _saveTransaction() async {
    if (_isLoading) return;

    setState(() {
      _isLoading = true;
    });

    try {
      // Парсим сумму из текстового поля
      final amount = double.parse(_amountController.text.replaceAll(',', '.'));
      
      // Парсим сумму для цели, если она указана
      double? amountToGoal;
      if (_showAmountToGoalField && _amountToGoalController.text.isNotEmpty) {
        amountToGoal = double.parse(_amountToGoalController.text.replaceAll(',', '.'));
        
        // Проверяем, что сумма для цели не превышает сумму дохода
        if (amountToGoal > amount) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Сумма для цели не может превышать сумму дохода')),
          );
          setState(() {
            _isLoading = false;
          });
          return;
        }
      }

      // Создаем обновленную транзакцию
      final updatedTransaction = Transaction(
        id: widget.transaction.id,
        amount: _isExpense ? -amount.abs() : amount.abs(),
        categoryId: _selectedCategoryId!,
        categoryName: _selectedCategoryObject?.name,
        date: _selectedDate,
        description: _descriptionController.text,
        goalId: _selectedGoalId,
        amountToGoal: amountToGoal,
        userId: widget.transaction.userId,
        createdAt: widget.transaction.createdAt,
      );

      // Вызываем провайдер для обновления транзакции
      final transactionProvider = Provider.of<TransactionProvider>(context, listen: false);
      await transactionProvider.updateTransaction(updatedTransaction);

      // Обновляем цели, если нужно
      if (_selectedGoalId != null) {
        final goalProvider = Provider.of<GoalProvider>(context, listen: false);
        await goalProvider.fetchGoals();
      }

      // Показываем сообщение об успехе и возвращаемся на предыдущий экран
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Транзакция успешно обновлена')),
        );
        Navigator.of(context).pop(true);
      }
    } catch (e) {
      // Обработка ошибок
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Ошибка: ${e.toString()}')),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  void _deleteTransaction() async {
     final confirm = await showDialog<bool>(
       context: context,
       builder: (BuildContext context) {
         return AlertDialog(
           backgroundColor: AppTheme.cardColor,
           title: Text('Подтверждение', style: TextStyle(color: AppTheme.textColor)),
           content: Text('Вы уверены, что хотите удалить эту транзакцию?', style: TextStyle(color: AppTheme.textColor)),
           actions: <Widget>[
             TextButton(
               child: Text('Отмена', style: TextStyle(color: AppTheme.accentColor)),
               onPressed: () => Navigator.of(context).pop(false),
             ),
             TextButton(
               child: Text('Удалить', style: TextStyle(color: Colors.red)),
               onPressed: () => Navigator.of(context).pop(true),
             ),
           ],
         );
       },
     );

     if (confirm == true) {
       setState(() { _isLoading = true; });
       try {
         final success = await Provider.of<TransactionProvider>(context, listen: false)
             .deleteTransaction(widget.transaction.id!, context);

         if (success) {
           ScaffoldMessenger.of(context).showSnackBar(
             SnackBar(content: Text('Транзакция удалена'), backgroundColor: Colors.green),
           );
           Navigator.of(context).pop();
         } else {
           ScaffoldMessenger.of(context).showSnackBar(
             SnackBar(content: Text('Ошибка удаления транзакции'), backgroundColor: Colors.red),
           );
         }
       } catch (e) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Произошла ошибка: $e'), backgroundColor: Colors.red),
          );
       } finally {
          setState(() { _isLoading = false; });
       }
     }
   }

  @override
  Widget build(BuildContext context) {
    final categoryProvider = Provider.of<CategoryProvider>(context);
    final goalProvider = Provider.of<GoalProvider>(context);

    final categories = _isExpense
        ? categoryProvider.categories.where((c) => !c.isIncome).toList()
        : categoryProvider.categories.where((c) => c.isIncome).toList();

    final validSelectedCategoryId = categories.any((cat) => cat.id == _selectedCategoryId)
        ? _selectedCategoryId
        : null;

    final Goal? currentGoal = _selectedGoalId != null
        ? goalProvider.goals.firstWhereOrNull((g) => g.id == _selectedGoalId)
        : null;

    return Scaffold(
      backgroundColor: AppTheme.cardColor,
      appBar: AppBar(
        title: Text(_isEditing ? 'Редактировать' : 'Детали транзакции'),
        backgroundColor: AppTheme.backgroundColor,
        elevation: 0,
        actions: [
          if (_isEditing)
            IconButton(
              icon: Icon(Icons.cancel_outlined),
              onPressed: () {
                setState(() {
                  _isEditing = false;
                  _amountController.text = widget.transaction.amount.abs().toStringAsFixed(2);
                  _descriptionController.text = widget.transaction.description ?? '';
                  _selectedDate = widget.transaction.date;
                  _selectedCategoryId = widget.transaction.categoryId;
                  _selectedGoalId = widget.transaction.goalId;
                  _amountToGoalController.text = widget.transaction.amountToGoal?.toStringAsFixed(2) ?? '';
                  _updateAmountToGoalVisibility(initialize: true);
                });
              },
            )
          else
            IconButton(
              icon: Icon(Icons.edit_outlined),
              onPressed: () => setState(() => _isEditing = true),
            ),
          IconButton(
            icon: Icon(Icons.delete_outline, color: Colors.red),
            onPressed: _isLoading ? null : _deleteTransaction,
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: ListView(
          children: [
            _buildDetailRow(
              label: 'Сумма',
              child: _isEditing
                  ? TextFormField(
                      controller: _amountController,
                      decoration: _inputDecoration('').copyWith(
                        prefixText: _isExpense ? '- ' : '+ ',
                        prefixStyle: TextStyle(
                          color: _isExpense ? Colors.redAccent : Colors.green,
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      style: TextStyle(
                        color: _isExpense ? Colors.redAccent : Colors.green,
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                      keyboardType: TextInputType.numberWithOptions(decimal: true),
                      inputFormatters: [
                        FilteringTextInputFormatter.allow(RegExp(r'^\d*[,.]?\d{0,2}')),
                      ],
                      onChanged: _onAmountChanged,
                    )
                  : Text(
                      '${widget.transaction.isIncome ? '+' : '-'} ${widget.transaction.amount.abs().toStringAsFixed(2)} ₽',
                      style: TextStyle(
                        color: widget.transaction.isIncome ? Colors.green : Colors.redAccent,
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
            ),
            SizedBox(height: 16),

            if (_isEditing)
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  ChoiceChip(
                    label: Text('Расход'),
                    selected: _isExpense,
                    onSelected: (selected) {
                      if (selected) {
                        setState(() {
                          _isExpense = true;
                          _selectedCategoryId = null;
                          _selectedCategoryObject = null;
                          _updateAmountToGoalVisibility();
                        });
                      }
                    },
                    selectedColor: Colors.redAccent.withOpacity(0.7),
                    labelStyle: TextStyle(color: _isExpense ? Colors.white : AppTheme.textColor),
                  ),
                  SizedBox(width: 10),
                  ChoiceChip(
                    label: Text('Доход'),
                    selected: !_isExpense,
                    onSelected: (selected) {
                      if (selected) {
                        setState(() {
                          _isExpense = false;
                          _selectedCategoryId = null;
                          _selectedCategoryObject = null;
                          _updateAmountToGoalVisibility();
                        });
                      }
                    },
                    selectedColor: Colors.green.withOpacity(0.7),
                    labelStyle: TextStyle(color: !_isExpense ? Colors.white : AppTheme.textColor),
                  ),
                ],
              ),
            if (_isEditing) SizedBox(height: 16),

            _buildDetailRow(
              label: 'Категория',
              child: _isEditing
                  ? Container(
                      padding: EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                      decoration: BoxDecoration(
                        color: AppTheme.secondaryCardColor,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: DropdownButtonHideUnderline(
                        child: DropdownButton<int>(
                          value: validSelectedCategoryId,
                          isExpanded: true,
                          dropdownColor: AppTheme.secondaryCardColor,
                          icon: Icon(Icons.arrow_drop_down, color: AppTheme.textColor),
                          hint: Text('Выберите категорию', style: TextStyle(color: AppTheme.textColor.withOpacity(0.5))),
                          items: categories.map((models.Category category) {
                            return DropdownMenuItem<int>(
                              value: category.id,
                              child: Row(
                                children: [
                                  Icon(category.icon, color: category.color, size: 20),
                                  SizedBox(width: 8),
                                  Text(category.name, style: TextStyle(color: AppTheme.textColor)),
                                ],
                              ),
                            );
                          }).toList(),
                          onChanged: (int? newCategoryId) {
                            setState(() {
                              _selectedCategoryId = newCategoryId;
                              if (newCategoryId != null) {
                                 _selectedCategoryObject = categories.firstWhere((cat) => cat.id == newCategoryId);
                              } else {
                                 _selectedCategoryObject = null;
                              }
                            });
                          },
                        ),
                      ),
                    )
                  : Text(
                      _selectedCategoryObject?.name ?? widget.transaction.categoryName ?? 'Неизвестно',
                      style: TextStyle(color: AppTheme.textColor, fontSize: 16),
                    ),
            ),
             SizedBox(height: 16),

            _buildDetailRow(
              label: 'Цель',
              child: _isEditing
                  ? GoalDropdown(
                      selectedGoal: currentGoal,
                      onChanged: (Goal? goal) {
                        setState(() {
                          _selectedGoalId = goal?.id;
                          _updateAmountToGoalVisibility();
                        });
                      },
                      allowNull: true,
                    )
                  : Text(
                      currentGoal?.name ?? 'Нет цели',
                      style: TextStyle(color: AppTheme.textColor, fontSize: 16),
                    ),
            ),
            _selectedGoalId != null ? SizedBox(height: 16) : SizedBox.shrink(),

            if (_isEditing && _showAmountToGoalField)
              _buildDetailRow(
                label: 'Сумма для цели',
                child: TextFormField(
                  controller: _amountToGoalController,
                  decoration: _inputDecoration(''),
                  style: TextStyle(color: AppTheme.textColor, fontSize: 16),
                  keyboardType: TextInputType.numberWithOptions(decimal: true),
                  inputFormatters: [
                    FilteringTextInputFormatter.allow(RegExp(r'^\d*[,.]?\d{0,2}')),
                  ],
                  validator: (value) {
                    if (value != null && value.isNotEmpty) {
                      try {
                        double parsedValue = double.parse(value.replaceAll(',', '.'));
                        if (parsedValue < 0) return 'Не может быть < 0';
                        
                        // Проверяем, что сумма для цели не превышает общую сумму
                        double totalAmount = double.tryParse(_amountController.text.replaceAll(',', '.')) ?? 0;
                        if (parsedValue > totalAmount) return 'Не может быть > суммы дохода';
                      } catch (e) {
                        return 'Некорректно';
                      }
                    }
                    return null;
                  },
                ),
              ),
            _isEditing && _showAmountToGoalField ? SizedBox(height: 16) : SizedBox.shrink(),

            _buildDetailRow(
              label: 'Дата',
              child: InkWell(
                onTap: () => _selectDate(context),
                child: Row(
                  children: [
                    Icon(Icons.calendar_today_outlined, size: 16, color: AppTheme.secondaryTextColor),
                    SizedBox(width: 8),
                    Text(
                      DateFormat('dd MMMM yyyy', 'ru').format(_selectedDate),
                      style: TextStyle(color: AppTheme.textColor, fontSize: 16),
                    ),
                    if (_isEditing) Icon(Icons.arrow_drop_down, color: AppTheme.secondaryTextColor),
                  ],
                ),
              ),
            ),
            SizedBox(height: 16),

            _buildDetailRow(
              label: 'Описание',
              child: _isEditing
                  ? TextFormField(
                      controller: _descriptionController,
                      decoration: _inputDecoration(''),
                      style: TextStyle(color: AppTheme.textColor, fontSize: 16),
                      maxLines: null,
                    )
                  : Text(
                      widget.transaction.description?.isNotEmpty == true ? widget.transaction.description! : 'Нет описания',
                      style: TextStyle(color: AppTheme.textColor, fontSize: 16),
                    ),
            ),
            SizedBox(height: 30),

            if (_isEditing)
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: _isLoading ? null : _saveTransaction,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.accentColor,
                    foregroundColor: Colors.black,
                    padding: EdgeInsets.symmetric(vertical: 16),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                  ),
                  child: _isLoading
                      ? SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.black))
                      : Text('Сохранить изменения'),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildDetailRow({required String label, required Widget child}) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: TextStyle(color: AppTheme.secondaryTextColor, fontSize: 14),
        ),
        SizedBox(height: 4),
        child,
        Divider(color: AppTheme.secondaryTextColor.withOpacity(0.2), height: 16),
      ],
    );
  }

  InputDecoration _inputDecoration(String labelText) {
    return InputDecoration(
      labelText: labelText,
      labelStyle: TextStyle(color: AppTheme.secondaryTextColor),
      filled: true,
      fillColor: AppTheme.secondaryCardColor,
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide.none),
      contentPadding: EdgeInsets.symmetric(vertical: 12, horizontal: 16),
    );
  }
} 