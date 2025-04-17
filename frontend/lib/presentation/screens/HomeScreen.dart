import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../../providers/transaction_provider.dart';
import '../../providers/category_provider.dart';
import '../../providers/goal_provider.dart';
import '../../providers/auth_provider.dart';
import '../theme/app_theme.dart';
import '../../models/transaction.dart';
import '../../models/goal.dart';
import '../../models/category.dart';
import 'package:focus_detector/focus_detector.dart';
import 'TransactionDetailScreen.dart';
import 'GoalsScreen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  _HomeScreenState createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with WidgetsBindingObserver {
  bool _isLoading = true;
  String? _error;
  bool _isInitialized = false;
  final FocusNode _focusNode = FocusNode();
  int _displayedTransactionsCount = 8; // Показываем 8 транзакций по умолчанию

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _loadData();
  }
  
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      // Загружаем данные при возвращении к приложению
      _loadData();
    }
  }
  
  Future<void> _loadData() async {
    if (!mounted) return;
    
    setState(() {
      _isLoading = true;
      _error = null;
    });
    
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    if (!authProvider.isAuthenticated) {
      setState(() {
        _isLoading = false;
      });
      return;
    }

    try {
      // Загружаем данные параллельно
      final transactionProvider = Provider.of<TransactionProvider>(context, listen: false);
      final categoryProvider = Provider.of<CategoryProvider>(context, listen: false);
      final goalProvider = Provider.of<GoalProvider>(context, listen: false);

      await Future.wait([
        transactionProvider.fetchTransactions(),
        categoryProvider.fetchCategories(),
        goalProvider.fetchGoals(),
      ]);
      
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _error = e.toString();
        });
      }
      print('Ошибка при загрузке данных: $e');
    }
  }

  void _loadMoreTransactions() {
    setState(() {
      _displayedTransactionsCount += 8; // Увеличиваем количество отображаемых транзакций
    });
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return FocusDetector(
      onFocusGained: () {
        // Обновляем данные при возвращении на экран
        _loadData();
      },
      child: Scaffold(
        appBar: AppBar(
          title: Text('Главная'),
          actions: [
            IconButton(
              icon: Icon(Icons.person_outline),
              onPressed: () {
                Navigator.pushNamed(context, '/profile');
              },
            ),
          ],
        ),
        body: _isLoading
            ? Center(child: CircularProgressIndicator(color: AppTheme.primaryColor))
            : _error != null
              ? _buildErrorWidget()
              : RefreshIndicator(
                  onRefresh: _loadData,
                  color: AppTheme.primaryColor,
                  child: _buildHomeContent(),
                ),
      ),
    );
  }
  
  Widget _buildErrorWidget() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.error_outline, color: Colors.red, size: 48),
          SizedBox(height: 16),
          Text(
            'Ошибка загрузки данных',
            style: TextStyle(color: AppTheme.textColor, fontSize: 18),
          ),
          SizedBox(height: 8),
          Text(
            _error ?? 'Неизвестная ошибка',
            style: TextStyle(color: AppTheme.secondaryTextColor),
            textAlign: TextAlign.center,
          ),
          SizedBox(height: 16),
          ElevatedButton(
            onPressed: _loadData,
            child: Text('Повторить'),
          ),
        ],
      ),
    );
  }

  Widget _buildHomeContent() {
    final transactionProvider = Provider.of<TransactionProvider>(context);
    final transactions = transactionProvider.transactions;
    
    // Рассчитываем общий баланс и расходы
    double totalBalance = 0;
    double totalExpenses = 0;
    
    for (var transaction in transactions) {
      if (transaction.amount > 0) {
        totalBalance += transaction.amount;
      } else {
        totalExpenses += transaction.amount.abs();
        totalBalance += transaction.amount; // Добавляем отрицательное значение
      }
    }
    
    return SingleChildScrollView(
      padding: EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Верхняя секция с балансом, расходами и целями
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Секция с балансом и расходами (левая часть)
              Expanded(
                flex: 3,
                child: _buildFinancialSummary(totalBalance, totalExpenses),
              ),
              SizedBox(width: 16),
              // Секция с целями (правая часть)
              Expanded(
                flex: 2,
                child: _buildGoalsSection(),
              ),
            ],
          ),
          SizedBox(height: 24),
          
          // Заголовок "Последние транзакции"
          Text(
            'Последние транзакции',
            style: TextStyle(
              color: AppTheme.textColor,
              fontSize: 18,
              fontWeight: FontWeight.bold,
            ),
          ),
          SizedBox(height: 16),
          
          // Список последних транзакций
          _buildRecentTransactions(transactions),
          
          // Кнопка "Загрузить еще"
          if (transactions.length > _displayedTransactionsCount)
            Center(
              child: TextButton(
                onPressed: _loadMoreTransactions,
                child: Text('Загрузить еще'),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildFinancialSummary(double balance, double expenses) {
    return Container(
      padding: EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppTheme.cardColor,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Счет
          Text(
            'Счет',
            style: TextStyle(
              color: AppTheme.secondaryTextColor,
              fontSize: 14,
            ),
          ),
          SizedBox(height: 4),
          Text(
            '${NumberFormat('#,###').format(balance)} ₽',
            style: TextStyle(
              color: AppTheme.textColor,
              fontSize: 24,
              fontWeight: FontWeight.bold,
            ),
          ),
          SizedBox(height: 16),
          
          // Траты
          Text(
            'Траты',
            style: TextStyle(
              color: AppTheme.secondaryTextColor,
              fontSize: 14,
            ),
          ),
          SizedBox(height: 4),
          Text(
            '${NumberFormat('#,###').format(expenses)} ₽',
            style: TextStyle(
              color: AppTheme.textColor,
              fontSize: 24,
              fontWeight: FontWeight.bold,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildGoalsSection() {
    final goalProvider = Provider.of<GoalProvider>(context);
    final goals = goalProvider.goals;
    
    // Сортируем цели по прогрессу (от наибольшего к наименьшему)
    final sortedGoals = List<Goal>.from(goals);
    sortedGoals.sort((a, b) => b.progress.compareTo(a.progress));
    
    // Берем только топ-3 цели для отображения
    final topGoals = sortedGoals.take(3).toList();
    
    return GestureDetector(
      onTap: () {
        // Переходим на экран целей при нажатии на секцию
        Navigator.push(
          context,
          MaterialPageRoute(builder: (context) => GoalsScreen()),
        );
      },
      child: Container(
        padding: EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppTheme.cardColor,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Цели и задачи',
                  style: TextStyle(
                    color: AppTheme.textColor,
                    fontWeight: FontWeight.bold,
                    fontSize: 16,
                  ),
                ),
                Icon(
                  Icons.arrow_forward_ios,
                  color: AppTheme.secondaryTextColor,
                  size: 16,
                ),
              ],
            ),
            SizedBox(height: 16),
            if (topGoals.isEmpty)
              Center(
                child: Text(
                  'Нет активных целей',
                  style: TextStyle(color: AppTheme.secondaryTextColor),
                ),
              )
            else
              ...topGoals.map((goal) => _buildGoalItem(goal)).toList(),
          ],
        ),
      ),
    );
  }

  Widget _buildGoalItem(Goal goal) {
    return Container(
      margin: EdgeInsets.only(bottom: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(
                  goal.name,
                  style: TextStyle(
                    color: AppTheme.textColor,
                    fontWeight: FontWeight.bold,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              Text(
                '${(goal.progress * 100).toInt()}%',
                style: TextStyle(
                  color: AppTheme.primaryColor,
                ),
              ),
            ],
          ),
          SizedBox(height: 8),
          LinearProgressIndicator(
            value: goal.progress,
            backgroundColor: AppTheme.secondaryCardColor,
            valueColor: AlwaysStoppedAnimation<Color>(AppTheme.primaryColor),
          ),
          SizedBox(height: 4),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '${NumberFormat('#,###').format(goal.currentAmount)} ₽',
                style: TextStyle(
                  color: AppTheme.secondaryTextColor,
                  fontSize: 12,
                ),
              ),
              Text(
                '${NumberFormat('#,###').format(goal.targetAmount)} ₽',
                style: TextStyle(
                  color: AppTheme.secondaryTextColor,
                  fontSize: 12,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildRecentTransactions(List<Transaction> transactions) {
    if (transactions.isEmpty) {
      return Center(
        child: Text(
          'Нет транзакций',
          style: TextStyle(color: AppTheme.textColor),
        ),
      );
    }
    
    // Ограничиваем список отображаемыми транзакциями
    final displayedTransactions = transactions.take(_displayedTransactionsCount).toList();
    
    return Column(
      children: displayedTransactions.map((transaction) => _buildTransactionItem(transaction)).toList(),
    );
  }

  Widget _buildTransactionItem(Transaction transaction) {
    // Получаем категорию для транзакции
    final categoryProvider = Provider.of<CategoryProvider>(context, listen: false);
    final category = categoryProvider.getCategoryById(transaction.categoryId);
    
    // Определяем иконку в зависимости от категории
    IconData iconData;
    if (transaction.amount > 0) {
      iconData = Icons.work; // Иконка для доходов
    } else if (category?.name?.toLowerCase().contains('продукт') ?? false) {
      iconData = Icons.shopping_cart; // Иконка для продуктов
    } else if (category?.name?.toLowerCase().contains('транспорт') ?? false) {
      iconData = Icons.directions_car; // Иконка для транспорта
    } else if (category?.name?.toLowerCase().contains('зарплат') ?? false) {
      iconData = Icons.account_balance_wallet; // Иконка для зарплаты
    } else {
      iconData = category?.icon ?? Icons.help_outline; // Иконка по умолчанию или из категории
    }
    
    return GestureDetector(
      onTap: () {
        // Открываем экран деталей транзакции
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => TransactionDetailScreen(transaction: transaction),
          ),
        );
      },
      child: Container(
        margin: EdgeInsets.symmetric(vertical: 4),
        padding: EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppTheme.cardColor,
          borderRadius: BorderRadius.circular(8),
        ),
        child: Row(
          children: [
            // Иконка категории
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                color: category?.color ?? AppTheme.secondaryCardColor,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Icon(
                iconData,
                color: Colors.white,
                size: 24,
              ),
            ),
            SizedBox(width: 16),
            // Информация о транзакции
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    category?.name ?? 'Неизвестная категория',
                    style: TextStyle(
                      color: AppTheme.textColor,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  Text(
                    DateFormat('yyyy-MM-dd').format(transaction.date),
                    style: TextStyle(
                      color: AppTheme.secondaryTextColor,
                      fontSize: 12,
                    ),
                  ),
                ],
              ),
            ),
            // Сумма транзакции
            Text(
              '${transaction.amount > 0 ? '+' : ''}${NumberFormat('#,###').format(transaction.amount)} ₽',
              style: TextStyle(
                color: transaction.amount > 0 ? Colors.green : AppTheme.textColor,
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ),
      ),
    );
  }
}