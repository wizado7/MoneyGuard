import 'package:flutter/material.dart';
import 'profile_screen.dart'; // استيراد شاشة ملف الشخصي

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(
          'Деньги',
          style: TextStyle(color: Colors.white), // جعل النص أبيض
        ),
        backgroundColor: Colors.black, // خلفية سوداء
        actions: [
          IconButton(
            icon: Icon(Icons.search, color: Colors.white), // جعل أيقونة البحث بيضاء
            onPressed: () {
              // تنفيذ عملية البحث
            },
          ),
          IconButton(
            icon: Icon(Icons.person, color: Colors.white), // جعل أيقونة الملف الشخصي بيضاء
            onPressed: () {
              // تحويل المستخدم إلى صفحة الملف الشخصي
              Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => ProfileScreen()),
              );
            },
          ),
        ],
      ),
      body: Container(
        color: Colors.black, // خلفية سوداء
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // القسم الأول: الرصيد الحالي
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Траты',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 16,
                        ),
                      ),
                      SizedBox(height: 8),
                      Text(
                        '25 450 ₽',
                        style: TextStyle(
                          fontSize: 24,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                      ),
                    ],
                  ),
                  Container(
                    width: 100,
                    height: 100,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(8),
                      color: Colors.grey[800],
                    ),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          'Продукты',
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 14,
                          ),
                        ),
                        SizedBox(height: 8),
                        Text(
                          'Транспорт',
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 14,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              SizedBox(height: 24),

              // القسم الثاني: الشهر الحالي
              Text(
                'Текущий месяц',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                ),
              ),
              SizedBox(height: 8),
              Text(
                '6750 ₽',
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
              SizedBox(height: 24),

              // القسم الثالث: المعاملات الأخيرة
              Text(
                'Последние транзакции',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
              SizedBox(height: 8),
              Expanded(
                child: ListView.builder(
                  itemCount: 5, // عدد المعاملات
                  itemBuilder: (context, index) {
                    final transactions = [
                      {
                        'icon': Icons.shopping_cart, // أيقونة العملية
                        'title': 'Покупка продуктов', // اسم العملية
                        'date': '2023-08-15', // تاريخ العملية
                        'amount': '-500 ₽', // القيمة
                      },
                      {
                        'icon': Icons.directions_car,
                        'title': 'Оплата за транспорт',
                        'date': '2023-08-14',
                        'amount': '-150 ₽',
                      },
                      {
                        'icon': Icons.account_balance_wallet,
                        'title': 'Зарплата',
                        'date': '2023-08-10',
                        'amount': '+3000 ₽',
                      },
                      {
                        'icon': Icons.shopping_cart,
                        'title': 'Покупка продуктов',
                        'date': '2023-08-15',
                        'amount': '-500 ₽',
                      },
                      {
                        'icon': Icons.directions_car,
                        'title': 'Оплата за транспорт',
                        'date': '2023-08-14',
                        'amount': '-150 ₽',
                      },
                      {
                        'icon': Icons.account_balance_wallet,
                        'title': 'Зарплата',
                        'date': '2023-08-10',
                        'amount': '+3000 ₽',
                      },
                    ];

                    return ListTile(
                      leading: Icon(
                        transactions[index]['icon'] as IconData,
                      ),
                      title: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            transactions[index]['title'] as String,
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 16,
                            ),
                          ),
                          Text(
                            transactions[index]['date'] as String,
                            style: TextStyle(
                              color: Colors.grey[400],
                              fontSize: 12,
                            ),
                          ),
                        ],
                      ),
                      trailing: Text(
                        transactions[index]['amount'] as String,
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 16,
                        ),
                      ),
                    );
                  },
                ),
              ),
            ],
          ),
        ),
      ),
      bottomNavigationBar: BottomNavigationBar(
        type: BottomNavigationBarType.fixed,
        backgroundColor: Colors.black,
        selectedItemColor: Colors.white,
        unselectedItemColor: Colors.grey,
        items: [
          BottomNavigationBarItem(
            icon: Icon(Icons.home_outlined),
            label: '',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.add),
            label: '',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.check),
            label: '',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.chat_bubble_outline),
            label: '',
          ),
        ],
      ),
    );
  }
}