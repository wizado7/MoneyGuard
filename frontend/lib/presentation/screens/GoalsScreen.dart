import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../theme/app_theme.dart';
import '../../providers/goal_provider.dart';
import '../../models/goal.dart';
import 'AddGoalScreen.dart';
import 'EditGoalScreen.dart';

class GoalsScreen extends StatefulWidget {
  const GoalsScreen({Key? key}) : super(key: key);

  @override
  _GoalsScreenState createState() => _GoalsScreenState();
}

class _GoalsScreenState extends State<GoalsScreen> {
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _loadGoals();
  }

  Future<void> _loadGoals() async {
    setState(() {
      _isLoading = true;
    });

    try {
      await Provider.of<GoalProvider>(context, listen: false).fetchGoals();
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Ошибка при загрузке целей: ${e.toString()}'),
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
    final goalProvider = Provider.of<GoalProvider>(context);
    final goals = goalProvider.goals;

    return Scaffold(
      appBar: AppBar(
        title: Text('Цели и задачи'),
        leading: IconButton(
          icon: Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: RefreshIndicator(
        onRefresh: _loadGoals,
        child: _isLoading
            ? Center(child: CircularProgressIndicator())
            : goals.isEmpty
                ? Center(
                    child: Text(
                      'У вас пока нет целей',
                      style: TextStyle(color: AppTheme.textColor),
                    ),
                  )
                : ListView.builder(
                    padding: EdgeInsets.all(16),
                    itemCount: goals.length,
                    itemBuilder: (context, index) {
                      return _buildGoalItem(goals[index]);
                    },
                  ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          Navigator.push(
            context,
            MaterialPageRoute(builder: (context) => AddGoalScreen()),
          ).then((_) => _loadGoals());
        },
        backgroundColor: AppTheme.primaryColor,
        child: Icon(Icons.add, color: Colors.black),
      ),
    );
  }

  Widget _buildGoalItem(Goal goal) {
    return GestureDetector(
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => EditGoalScreen(goal: goal),
          ),
        ).then((_) => _loadGoals());
      },
      child: Container(
        margin: EdgeInsets.only(bottom: 16),
        padding: EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppTheme.cardColor,
          borderRadius: BorderRadius.circular(8),
        ),
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
                      fontSize: 16,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                Text(
                  '${(goal.progress * 100).toInt()}%',
                  style: TextStyle(
                    color: AppTheme.primaryColor,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
            SizedBox(height: 12),
            LinearProgressIndicator(
              value: goal.progress,
              backgroundColor: AppTheme.secondaryCardColor,
              valueColor: AlwaysStoppedAnimation<Color>(AppTheme.primaryColor),
              minHeight: 8,
            ),
            SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  '${NumberFormat('#,###').format(goal.currentAmount)} ₽',
                  style: TextStyle(
                    color: AppTheme.secondaryTextColor,
                  ),
                ),
                Text(
                  '${NumberFormat('#,###').format(goal.targetAmount)} ₽',
                  style: TextStyle(
                    color: AppTheme.secondaryTextColor,
                  ),
                ),
              ],
            ),
            if (goal.targetDate != null) ...[
              SizedBox(height: 8),
              Text(
                'Срок: ${DateFormat('yyyy-MM-dd').format(goal.targetDate!)}',
                style: TextStyle(
                  color: AppTheme.secondaryTextColor,
                  fontSize: 12,
                ),
              ),
            ],
            if (goal.priority != null && goal.priority!.isNotEmpty) ...[
              SizedBox(height: 8),
              Row(
                children: [
                  Text(
                    'Приоритет: ',
                    style: TextStyle(
                      color: AppTheme.secondaryTextColor,
                      fontSize: 12,
                    ),
                  ),
                  _buildPriorityBadge(goal.priority),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildPriorityBadge(String? priority) {
    if (priority == null || priority.isEmpty) return SizedBox.shrink();
    
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
    
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: priorityColor.withOpacity(0.2),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        displayText,
        style: TextStyle(
          color: priorityColor,
          fontSize: 12,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
} 