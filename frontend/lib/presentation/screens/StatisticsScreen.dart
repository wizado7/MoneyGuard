import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/app_theme.dart';
import '../../providers/statistics_provider.dart';

class StatisticsScreen extends StatefulWidget {
  const StatisticsScreen({super.key});

  @override
  State<StatisticsScreen> createState() => _StatisticsScreenState();
}

class _StatisticsScreenState extends State<StatisticsScreen> {
  String _selectedPeriod = 'Месяц';
  final List<String> _periods = ['Неделя', 'Месяц', 'Год'];

  @override
  void initState() {
    super.initState();
    // Загружаем статистику при открытии экрана
    Future.microtask(() {
      Provider.of<StatisticsProvider>(context, listen: false)
          .fetchStatistics(period: _selectedPeriod.toLowerCase());
    });
  }

  void _changePeriod(String period) {
    setState(() {
      _selectedPeriod = period;
    });
    
    // Загружаем статистику для выбранного периода
    Provider.of<StatisticsProvider>(context, listen: false)
        .fetchStatistics(period: period.toLowerCase());
  }

  @override
  Widget build(BuildContext context) {
    final statisticsProvider = Provider.of<StatisticsProvider>(context);
    
    return Scaffold(
      appBar: AppBar(
        title: Text('Статистика'),
      ),
      body: statisticsProvider.isLoading
          ? Center(child: CircularProgressIndicator())
          : statisticsProvider.error != null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        'Ошибка загрузки статистики',
                        style: TextStyle(color: Colors.red),
                      ),
                      SizedBox(height: 16),
                      ElevatedButton(
                        onPressed: () {
                          statisticsProvider.fetchStatistics(
                              period: _selectedPeriod.toLowerCase());
                        },
                        child: Text('Повторить'),
                      ),
                    ],
                  ),
                )
              : SingleChildScrollView(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Выбор периода
                      Container(
                        padding: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                        decoration: AppTheme.cardDecoration,
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              'Период:',
                              style: TextStyle(
                                color: AppTheme.textColor,
                                fontSize: 16,
                              ),
                            ),
                            DropdownButton<String>(
                              value: _selectedPeriod,
                              dropdownColor: AppTheme.cardColor,
                              style: TextStyle(color: AppTheme.textColor),
                              underline: SizedBox(),
                              onChanged: (String? newValue) {
                                if (newValue != null) {
                                  _changePeriod(newValue);
                                }
                              },
                              items: _periods
                                  .map<DropdownMenuItem<String>>((String value) {
                                return DropdownMenuItem<String>(
                                  value: value,
                                  child: Text(value),
                                );
                              }).toList(),
                            ),
                          ],
                        ),
                      ),
                      SizedBox(height: 24),

                      // Общая статистика
                      Text(
                        'Общая статистика',
                        style: TextStyle(
                          color: AppTheme.textColor,
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      SizedBox(height: 8),
                      _buildStatisticCard(
                        'Доходы',
                        '${statisticsProvider.getTotalIncome().toStringAsFixed(0)} ₽',
                        Colors.green,
                      ),
                      SizedBox(height: 8),
                      _buildStatisticCard(
                        'Расходы',
                        '${statisticsProvider.getTotalExpense().toStringAsFixed(0)} ₽',
                        Colors.red,
                      ),
                      SizedBox(height: 8),
                      _buildStatisticCard(
                        'Баланс',
                        '${statisticsProvider.getBalance().toStringAsFixed(0)} ₽',
                        AppTheme.primaryColor,
                      ),
                      SizedBox(height: 24),

                      // Расходы по категориям
                      Text(
                        'Расходы по категориям',
                        style: TextStyle(
                          color: AppTheme.textColor,
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      SizedBox(height: 8),
                      ...statisticsProvider.getCategoryBreakdown().map((category) {
                        return _buildCategoryItem(
                          category['name'],
                          category['amount'],
                          category['percentage'],
                          _getCategoryColor(category['name']),
                        );
                      }),
                    ],
                  ),
                ),
    );
  }

  Widget _buildStatisticCard(String title, String amount, Color color) {
    return Container(
      padding: EdgeInsets.all(16),
      decoration: AppTheme.cardDecoration,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            title,
            style: TextStyle(
              color: AppTheme.textColor,
              fontSize: 16,
            ),
          ),
          Text(
            amount,
            style: TextStyle(
              color: color,
              fontSize: 16,
              fontWeight: FontWeight.bold,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCategoryItem(
      String category, double amount, double percentage, Color color) {
    return Container(
      margin: EdgeInsets.only(bottom: 8),
      padding: EdgeInsets.all(16),
      decoration: AppTheme.cardDecoration,
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Container(
                    width: 16,
                    height: 16,
                    decoration: BoxDecoration(
                      color: color,
                      shape: BoxShape.circle,
                    ),
                  ),
                  SizedBox(width: 8),
                  Text(
                    category,
                    style: TextStyle(
                      color: AppTheme.textColor,
                      fontSize: 16,
                    ),
                  ),
                ],
              ),
              Text(
                '${amount.toStringAsFixed(0)} ₽',
                style: TextStyle(
                  color: AppTheme.textColor,
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
          SizedBox(height: 8),
          LinearProgressIndicator(
            value: percentage,
            backgroundColor: AppTheme.secondaryCardColor,
            valueColor: AlwaysStoppedAnimation<Color>(color),
          ),
          SizedBox(height: 4),
          Align(
            alignment: Alignment.centerRight,
            child: Text(
              '${(percentage * 100).toInt()}%',
              style: TextStyle(
                color: AppTheme.secondaryTextColor,
                fontSize: 12,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Color _getCategoryColor(String category) {
    // Простая функция для получения цвета категории
    // В реальном приложении это должно быть связано с данными категории
    switch (category.toLowerCase()) {
      case 'продукты':
        return Colors.green;
      case 'транспорт':
        return Colors.blue;
      case 'развлечения':
        return Colors.purple;
      case 'здоровье':
        return Colors.red;
      case 'одежда':
        return Colors.orange;
      default:
        return Colors.grey;
    }
  }
} 