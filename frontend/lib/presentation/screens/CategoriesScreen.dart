import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../theme/app_theme.dart';
import '../../providers/category_provider.dart';
import '../../models/category.dart' as models;

class CategoriesScreen extends StatefulWidget {
  const CategoriesScreen({super.key});

  @override
  State<CategoriesScreen> createState() => _CategoriesScreenState();
}

class _CategoriesScreenState extends State<CategoriesScreen> with SingleTickerProviderStateMixin {
  final TextEditingController _nameController = TextEditingController();
  late TabController _tabController;
  IconData _selectedIcon = Icons.shopping_cart;
  Color _selectedColor = Colors.green;

  final List<IconData> _availableIcons = [
    Icons.shopping_cart,
    Icons.directions_car,
    Icons.movie,
    Icons.favorite,
    Icons.shopping_bag,
    Icons.restaurant,
    Icons.card_giftcard,
    Icons.attach_money,
    Icons.account_balance,
    Icons.work,
  ];

  final List<Color> _availableColors = [
    Colors.green,
    Colors.blue,
    Colors.purple,
    Colors.red,
    Colors.orange,
    Colors.teal,
    Colors.pink,
    Colors.amber,
    Colors.indigo,
    Colors.cyan,
  ];

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    
    // Загружаем категории при открытии экрана
    Future.microtask(() {
      Provider.of<CategoryProvider>(context, listen: false).fetchCategories();
    });
  }

  void _showAddCategoryDialog() {
    _nameController.clear();
    _selectedIcon = Icons.shopping_cart;
    _selectedColor = Colors.green;

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Добавить категорию'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: _nameController,
                decoration: AppTheme.inputDecoration('Название', Icons.category),
                style: TextStyle(color: AppTheme.textColor),
              ),
              SizedBox(height: 16),
              Text(
                'Выберите иконку',
                style: TextStyle(color: AppTheme.textColor),
              ),
              SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: _availableIcons.map((icon) {
                  return GestureDetector(
                    onTap: () {
                      setState(() {
                        _selectedIcon = icon;
                      });
                      Navigator.pop(context);
                      _showAddCategoryDialog();
                    },
                    child: Container(
                      padding: EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: _selectedIcon == icon
                            ? AppTheme.primaryColor
                            : AppTheme.cardColor,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Icon(
                        icon,
                        color: _selectedIcon == icon
                            ? Colors.black
                            : AppTheme.textColor,
                      ),
                    ),
                  );
                }).toList(),
              ),
              SizedBox(height: 16),
              Text(
                'Выберите цвет',
                style: TextStyle(color: AppTheme.textColor),
              ),
              SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: _availableColors.map((color) {
                  return GestureDetector(
                    onTap: () {
                      setState(() {
                        _selectedColor = color;
                      });
                      Navigator.pop(context);
                      _showAddCategoryDialog();
                    },
                    child: Container(
                      width: 32,
                      height: 32,
                      decoration: BoxDecoration(
                        color: color,
                        shape: BoxShape.circle,
                        border: Border.all(
                          color: _selectedColor == color
                              ? AppTheme.primaryColor
                              : Colors.transparent,
                          width: 2,
                        ),
                      ),
                    ),
                  );
                }).toList(),
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
              if (_nameController.text.isNotEmpty) {
                _addCategory(
                  _nameController.text,
                  _selectedIcon,
                  _selectedColor,
                  _tabController.index == 0 ? false : true,
                );
                Navigator.of(context).pop();
              } else {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('Пожалуйста, введите название категории'),
                    backgroundColor: Colors.red,
                  ),
                );
              }
            },
            child: Text('Добавить'),
          ),
        ],
      ),
    );
  }

  void _addCategory(String name, IconData icon, Color color, bool isIncome) async {
    final categoryProvider = Provider.of<CategoryProvider>(context, listen: false);
    
    // Преобразуем IconData в строку
    String iconName = _getIconName(icon);
    
    // Преобразуем Color в строку
    String colorHex = '#${color.value.toRadixString(16).substring(2)}';
    
    final category = models.Category(
      name: name,
      iconName: iconName,
      colorHex: colorHex,
      isIncome: isIncome,
    );
    
    final success = await categoryProvider.addCategory(category);
    
    if (!success && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(categoryProvider.error ?? 'Ошибка добавления категории'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  void _showEditCategoryDialog(models.Category category) {
    _nameController.text = category.name;
    _selectedIcon = IconData(category.iconCode, fontFamily: 'MaterialIcons');
    _selectedColor = category.color;

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Редактировать категорию'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: _nameController,
                decoration: AppTheme.inputDecoration('Название', Icons.category),
                style: TextStyle(color: AppTheme.textColor),
              ),
              SizedBox(height: 16),
              Text(
                'Выберите иконку',
                style: TextStyle(color: AppTheme.textColor),
              ),
              SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: _availableIcons.map((icon) {
                  return GestureDetector(
                    onTap: () {
                      setState(() {
                        _selectedIcon = IconData(category.iconCode, fontFamily: 'MaterialIcons');
                      });
                      Navigator.pop(context);
                      _showEditCategoryDialog(category);
                    },
                    child: Container(
                      padding: EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: _selectedIcon == IconData(category.iconCode, fontFamily: 'MaterialIcons')
                            ? AppTheme.primaryColor
                            : AppTheme.cardColor,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Icon(
                        IconData(category.iconCode, fontFamily: 'MaterialIcons'),
                        color: _selectedIcon == IconData(category.iconCode, fontFamily: 'MaterialIcons')
                            ? Colors.black
                            : AppTheme.textColor,
                      ),
                    ),
                  );
                }).toList(),
              ),
              SizedBox(height: 16),
              Text(
                'Выберите цвет',
                style: TextStyle(color: AppTheme.textColor),
              ),
              SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: _availableColors.map((color) {
                  return GestureDetector(
                    onTap: () {
                      setState(() {
                        _selectedColor = color;
                      });
                      Navigator.pop(context);
                      _showEditCategoryDialog(category);
                    },
                    child: Container(
                      width: 32,
                      height: 32,
                      decoration: BoxDecoration(
                        color: color,
                        shape: BoxShape.circle,
                        border: Border.all(
                          color: _selectedColor == color
                              ? AppTheme.primaryColor
                              : Colors.transparent,
                          width: 2,
                        ),
                      ),
                    ),
                  );
                }).toList(),
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
              if (_nameController.text.isNotEmpty) {
                _updateCategory(
                  category,
                  _nameController.text,
                  _selectedIcon,
                  _selectedColor,
                );
                Navigator.of(context).pop();
              } else {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('Пожалуйста, введите название категории'),
                    backgroundColor: Colors.red,
                  ),
                );
              }
            },
            child: Text('Сохранить'),
          ),
        ],
      ),
    );
  }

  void _updateCategory(models.Category category, String name, IconData icon, Color color) async {
    final categoryProvider = Provider.of<CategoryProvider>(context, listen: false);
    
    // Преобразуем IconData в строку
    String iconName = _getIconName(icon);
    
    // Преобразуем Color в строку
    String colorHex = '#${color.value.toRadixString(16).substring(2)}';
    
    final updatedCategory = models.Category(
      id: category.id,
      name: name,
      iconName: iconName,
      colorHex: colorHex,
      isIncome: category.isIncome,
      userId: category.userId,
    );
    
    final success = await categoryProvider.updateCategory(updatedCategory);
    
    if (!success && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(categoryProvider.error ?? 'Ошибка обновления категории'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  void _deleteCategory(models.Category category) async {
    final categoryProvider = Provider.of<CategoryProvider>(context, listen: false);
    
    final success = await categoryProvider.deleteCategory(category.id!);
    
    if (!success && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(categoryProvider.error ?? 'Ошибка удаления категории'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  String _getIconName(IconData icon) {
    if (icon == Icons.shopping_cart) return 'shopping_cart';
    if (icon == Icons.directions_car) return 'directions_car';
    if (icon == Icons.movie) return 'movie';
    if (icon == Icons.favorite) return 'favorite';
    if (icon == Icons.shopping_bag) return 'shopping_bag';
    if (icon == Icons.restaurant) return 'restaurant';
    if (icon == Icons.card_giftcard) return 'card_giftcard';
    if (icon == Icons.attach_money) return 'attach_money';
    if (icon == Icons.account_balance) return 'account_balance';
    if (icon == Icons.work) return 'work';
    return 'category';
  }

  @override
  Widget build(BuildContext context) {
    final categoryProvider = Provider.of<CategoryProvider>(context);
    
    return Scaffold(
      appBar: AppBar(
        title: Text('Категории'),
        bottom: TabBar(
          controller: _tabController,
          tabs: [
            Tab(text: 'Расходы'),
            Tab(text: 'Доходы'),
          ],
        ),
      ),
      body: categoryProvider.isLoading
          ? Center(child: CircularProgressIndicator())
          : categoryProvider.error != null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        'Ошибка загрузки категорий',
                        style: TextStyle(color: Colors.red),
                      ),
                      SizedBox(height: 16),
                      ElevatedButton(
                        onPressed: () {
                          categoryProvider.fetchCategories();
                        },
                        child: Text('Повторить'),
                      ),
                    ],
                  ),
                )
              : TabBarView(
                  controller: _tabController,
                  children: [
                    // Вкладка расходов
                    _buildCategoryList(categoryProvider.expenseCategories),
                    // Вкладка доходов
                    _buildCategoryList(categoryProvider.incomeCategories),
                  ],
                ),
      floatingActionButton: FloatingActionButton(
        onPressed: _showAddCategoryDialog,
        backgroundColor: AppTheme.primaryColor,
        foregroundColor: Colors.black,
        child: Icon(Icons.add),
      ),
    );
  }

  Widget _buildCategoryList(List<models.Category> categories) {
    if (categories.isEmpty) {
      return Center(
        child: Text(
          'Нет доступных категорий',
          style: TextStyle(color: AppTheme.textColor),
        ),
      );
    }
    
    return ListView.builder(
      itemCount: categories.length,
      itemBuilder: (context, index) {
        final category = categories[index];
        // Проверяем, является ли категория базовой (userId == null)
        final isBaseCategory = category.userId == null;
        
        return _buildCategoryItem(category, isBaseCategory);
      },
    );
  }

  Widget _buildCategoryItem(models.Category category, bool isBaseCategory) {
    return Container(
      margin: EdgeInsets.only(bottom: 8),
      decoration: AppTheme.cardDecoration,
      child: ListTile(
        leading: Container(
          padding: EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: category.color.withOpacity(0.2),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Icon(
            IconData(category.iconCode, fontFamily: 'MaterialIcons'),
            color: category.color,
          ),
        ),
        title: Text(
          category.name,
          style: TextStyle(color: AppTheme.textColor),
        ),
        // Если это базовая категория, не показываем кнопки редактирования и удаления
        trailing: isBaseCategory
            ? null
            : Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  IconButton(
                    icon: Icon(Icons.edit, color: AppTheme.textColor),
                    onPressed: () => _showEditCategoryDialog(category),
                  ),
                  IconButton(
                    icon: Icon(Icons.delete, color: Colors.red),
                    onPressed: () {
                      showDialog(
                        context: context,
                        builder: (context) => AlertDialog(
                          title: Text('Удалить категорию'),
                          content: Text(
                              'Вы уверены, что хотите удалить категорию "${category.name}"?'),
                          actions: [
                            TextButton(
                              onPressed: () => Navigator.pop(context),
                              child: Text('Отмена'),
                            ),
                            TextButton(
                              onPressed: () {
                                _deleteCategory(category);
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
      ),
    );
  }

  @override
  void dispose() {
    _nameController.dispose();
    _tabController.dispose();
    super.dispose();
  }
} 