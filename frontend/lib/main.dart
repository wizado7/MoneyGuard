import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:provider/provider.dart';
import 'package:moneyguard/providers/auth_provider.dart';
import 'package:moneyguard/providers/category_provider.dart';
import 'package:moneyguard/providers/transaction_provider.dart';
import 'package:moneyguard/providers/profile_provider.dart';
import 'package:moneyguard/providers/goal_provider.dart';
import 'package:moneyguard/providers/limit_provider.dart';
import 'package:moneyguard/providers/ai_chat_provider.dart';
import 'package:moneyguard/providers/statistics_provider.dart';
import 'package:moneyguard/presentation/screens/splash_screen.dart';
import 'package:moneyguard/presentation/screens/welcome_screen.dart';
import 'package:moneyguard/presentation/screens/main_navigation_screen.dart';
import 'package:moneyguard/presentation/theme/app_theme.dart';
// Импортируем остальные экраны для routes
import 'package:moneyguard/presentation/screens/login_screen.dart';
import 'package:moneyguard/presentation/screens/register_screen.dart';
import 'package:moneyguard/presentation/screens/profile_screen.dart';
import 'package:moneyguard/presentation/screens/EditProfileScreen.dart';
import 'package:moneyguard/presentation/screens/SettingsScreen.dart';
import 'package:moneyguard/presentation/screens/CategoriesScreen.dart';
import 'package:moneyguard/presentation/screens/GoalsScreen.dart';
import 'package:moneyguard/presentation/screens/LimitsScreen.dart';
import 'package:moneyguard/presentation/screens/StatisticsScreen.dart';
import 'package:moneyguard/presentation/screens/ChangePasswordScreen.dart';
import 'package:intl/intl.dart';
import 'package:intl/date_symbol_data_local.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try {
     print("Loading .env file...");
     await dotenv.load(fileName: ".env");
     print(".env file loaded successfully.");
     // print("API_BASE_URL from dotenv: ${dotenv.env['API_BASE_URL']}"); // Лог для проверки
  } catch (e) {
     print("Error loading .env file: $e");
  }
  
  // Инициализируем локаль для форматирования дат
  await initializeDateFormatting('ru_RU', null);
  Intl.defaultLocale = 'ru_RU';
  
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        // AuthProvider должен быть первым, т.к. другие могут от него зависеть
        ChangeNotifierProvider(create: (_) => AuthProvider()),
        // Используем ChangeNotifierProxyProvider для провайдеров,
        // которым нужен AuthProvider (например, для токена или статуса аутентификации)
        // Хотя ApiService сам добавляет токен, провайдерам может быть нужен статус auth.isAuthenticated
        ChangeNotifierProxyProvider<AuthProvider, CategoryProvider>(
           create: (_) => CategoryProvider(),
           update: (_, auth, previous) => (previous ?? CategoryProvider())..updateAuth(auth),
        ),
         ChangeNotifierProxyProvider<AuthProvider, TransactionProvider>(
           create: (_) => TransactionProvider(),
           update: (_, auth, previous) => (previous ?? TransactionProvider())..updateAuth(auth),
        ),
         ChangeNotifierProxyProvider<AuthProvider, ProfileProvider>(
           create: (_) => ProfileProvider(),
           update: (_, auth, previous) => (previous ?? ProfileProvider())..updateAuth(auth),
        ),
         ChangeNotifierProxyProvider<AuthProvider, GoalProvider>(
           create: (_) => GoalProvider(),
           update: (_, auth, previous) => (previous ?? GoalProvider())..updateAuth(auth),
        ),
         ChangeNotifierProxyProvider<AuthProvider, LimitProvider>(
           create: (_) => LimitProvider(),
           update: (_, auth, previous) => (previous ?? LimitProvider())..updateAuth(auth),
        ),
        // Эти провайдеры могут не зависеть напрямую от AuthProvider, но все равно используем Proxy для единообразия
         ChangeNotifierProxyProvider<AuthProvider, AIChatProvider>(
           create: (_) => AIChatProvider(),
           update: (_, auth, previous) => (previous ?? AIChatProvider()), // .updateAuth(auth) если нужно
        ),
         ChangeNotifierProxyProvider<AuthProvider, StatisticsProvider>(
           create: (_) => StatisticsProvider(),
           update: (_, auth, previous) => (previous ?? StatisticsProvider()), // .updateAuth(auth) если нужно
        ),
      ],
      child: MaterialApp(
        title: 'MoneyGuard',
        theme: AppTheme.darkTheme, // Используем вашу темную тему
        debugShowCheckedModeBanner: false,
        navigatorObservers: [ _KeyboardDismissNavigatorObserver() ], // Оставляем ваш обсервер
        home: Consumer<AuthProvider>(
          builder: (ctx, auth, _) {
            print("MyApp Consumer: isLoading=${auth.isLoading}, isAuthenticated=${auth.isAuthenticated}");
            if (auth.isLoading) {
              return const SplashScreen(); // Показываем сплэш во время проверки авто-входа
            } else if (auth.isAuthenticated) {
              return const MainNavigationScreen(); // Если аутентифицирован
            } else {
              return const WelcomeScreen(); // Если не аутентифицирован
            }
          },
        ),
        // Определяем маршруты для навигации по имени
        routes: {
          '/welcome': (context) => const WelcomeScreen(),
          '/home': (context) => const MainNavigationScreen(), // Главный экран после логина
          '/login': (context) => const LoginScreen(),
          '/register': (context) => const RegisterScreen(),
          '/profile': (context) => const ProfileScreen(),
          '/edit_profile': (context) => const EditProfileScreen(),
          '/settings': (context) => const SettingsScreen(),
          '/categories': (context) => const CategoriesScreen(),
          '/goals': (context) => const GoalsScreen(),
          '/limits': (context) => const LimitsScreen(),
          '/statistics': (context) => const StatisticsScreen(),
          '/change_password': (context) => const ChangePasswordScreen(),
          // Добавьте другие маршруты при необходимости
        },
      ),
    );
  }
}

// Вспомогательный класс для провайдеров, зависящих от AuthProvider
// Добавьте метод updateAuth в ваши провайдеры (CategoryProvider, TransactionProvider и т.д.)
/* Пример для CategoryProvider:
class CategoryProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();
  AuthProvider? _authProvider; // Храним ссылку на AuthProvider
  List<Category> _categories = [];
  bool _isLoading = false;
  String? _error;

  List<Category> get categories => _categories;
  bool get isLoading => _isLoading;
  String? get error => _error;

  // Метод для обновления ссылки на AuthProvider
  void updateAuth(AuthProvider auth) {
    _authProvider = auth;
    // Если пользователь аутентифицирован и категории еще не загружены, загружаем их
    if (auth.isAuthenticated && _categories.isEmpty) {
       fetchCategories();
    } else if (!auth.isAuthenticated) {
       // Если пользователь разлогинился, очищаем данные
       _categories = [];
       _error = null;
       notifyListeners();
    }
  }

  Future<void> fetchCategories() async {
    if (_authProvider == null || !_authProvider!.isAuthenticated) {
       _error = "Пользователь не аутентифицирован";
       notifyListeners();
       return;
    }
    _isLoading = true;
    _error = null;
    notifyListeners();
    try {
      _categories = await _apiService.getCategories();
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
*/

// Наблюдатель для скрытия клавиатуры (оставляем как есть)
class _KeyboardDismissNavigatorObserver extends NavigatorObserver {
   @override
   void didPush(Route<dynamic> route, Route<dynamic>? previousRoute) {
     FocusManager.instance.primaryFocus?.unfocus();
     super.didPush(route, previousRoute);
   }

   @override
   void didPop(Route<dynamic> route, Route<dynamic>? previousRoute) {
     FocusManager.instance.primaryFocus?.unfocus();
     super.didPop(route, previousRoute);
   }
}