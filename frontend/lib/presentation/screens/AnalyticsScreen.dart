import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/app_theme.dart';
import '../../providers/statistics_provider.dart';

class AnalyticsScreen extends StatefulWidget {
  const AnalyticsScreen({super.key});

  @override
  State<AnalyticsScreen> createState() => _AnalyticsScreenState();
}

class _AnalyticsScreenState extends State<AnalyticsScreen> {
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
        title: Text('Аналитика'),
        actions: [
          IconButton(
            icon: Icon(Icons.pie_chart),
            onPressed: () {
              Navigator.pushNamed(context, '/statistics');
            },
          ),
        ],
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
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(
                            'Общая статистика',
                            style: TextStyle(
                              color: AppTheme.textColor,
                              fontSize: 18,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          TextButton(
                            onPressed: () {
                              Navigator.pushNamed(context, '/statistics');
                            },
                            child: Text('Подробнее'),
                          ),
                        ],
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
                        return _buildCategoryStatisticCard(
                          category['name'],
                          '${category['amount'].toStringAsFixed(0)} ₽',
                          category['percentage'],
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

  Widget _buildCategoryStatisticCard(
      String category, String amount, double percentage) {
    return Container(
      margin: EdgeInsets.only(bottom: 8),
      padding: EdgeInsets.all(16),
      decoration: AppTheme.cardDecoration,
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                category,
                style: TextStyle(
                  color: AppTheme.textColor,
                  fontSize: 16,
                ),
              ),
              Text(
                amount,
                style: TextStyle(
                  color: AppTheme.textColor,
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
          SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: LinearProgressIndicator(
                  value: percentage,
                  backgroundColor: AppTheme.secondaryCardColor,
                  valueColor: AlwaysStoppedAnimation<Color>(AppTheme.primaryColor),
                ),
              ),
              SizedBox(width: 8),
              Text(
                '${(percentage * 100).toInt()}%',
                style: TextStyle(
                  color: AppTheme.textColor,
                  fontSize: 14,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
} 