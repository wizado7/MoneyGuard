import 'dart:convert';
import 'dart:io';
import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../models/auth_response.dart';
import '../models/user_profile.dart';
import '../models/category.dart';
import '../models/goal.dart';
import '../models/limit.dart';
import '../models/transaction.dart';
import 'package:moneyguard/config/config.dart';
import 'package:moneyguard/constants/storage_keys.dart';
import 'package:intl/intl.dart';
import 'package:http/http.dart' as http;

class AuthInterceptor extends Interceptor {
  final FlutterSecureStorage _storage;
  final Dio _dio;

  AuthInterceptor(this._storage, this._dio);

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) async {
    final publicPaths = ['/auth/login', '/auth/register', '/auth/refresh'];

    bool isPublicPath = publicPaths.any((path) => options.path.contains(path));

    print('>>> Interceptor: Requesting [${options.method}] ${options.baseUrl}${options.path}');
    print('>>> Interceptor: Headers: ${options.headers}');
    if (options.data != null) {
      print('>>> Interceptor: Request Body: ${options.data}');
    }

    if (!isPublicPath) {
      print('>>> Interceptor: Attempting to read token (${StorageKeys.jwtToken}) for protected path...');
      final token = await _storage.read(key: StorageKeys.jwtToken);

      if (token != null && token.isNotEmpty) {
        print('>>> Interceptor: Token found. Adding Authorization header.');
        options.headers['Authorization'] = 'Bearer $token';
      } else {
        print('>>> Interceptor: Token (${StorageKeys.jwtToken}) NOT found for protected path: ${options.path}');
      }
    } else {
       print('>>> Interceptor: Skipping Authorization header for public path: ${options.path}');
    }

    print('>>> Interceptor: Final Headers: ${options.headers}');
    return handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) async {
     print('<<< DioError Interceptor: Path: ${err.requestOptions.path}, Status: ${err.response?.statusCode}, Error: ${err.message}');
     print('<<< DioError Interceptor: Response Headers: ${err.response?.headers}');
     if (err.response?.data != null) {
        print('<<< DioError Interceptor: Response Body: ${err.response?.data}');
     }

    if (err.response?.statusCode == 401 && !err.requestOptions.path.contains('/auth/refresh')) {
       print('<<< Interceptor: Unauthorized (401). Attempting token refresh...');
       final refreshToken = await _storage.read(key: StorageKeys.refreshToken);

       if (refreshToken != null && refreshToken.isNotEmpty) {
         try {
           var refreshDio = Dio(BaseOptions(baseUrl: AppConfig.baseUrl));
           refreshDio.interceptors.add(LogInterceptor(requestBody: true, responseBody: true));

           print('<<< Interceptor: Sending refresh token request to /auth/refresh');
           final response = await refreshDio.post(
             '/auth/refresh',
             data: {'refreshToken': refreshToken},
           );

           if (response.statusCode == 200 && response.data != null && response.data['token'] != null) {
             final newAccessToken = response.data['token'];
             final newRefreshToken = response.data['refresh_token'];

             print('<<< Interceptor: Token refresh successful. Storing new tokens.');
             await _storage.write(key: StorageKeys.jwtToken, value: newAccessToken);
             if (newRefreshToken != null) {
                await _storage.write(key: StorageKeys.refreshToken, value: newRefreshToken);
             }

             print('<<< Interceptor: Retrying original request to ${err.requestOptions.path} with new token.');
             err.requestOptions.headers['Authorization'] = 'Bearer $newAccessToken';

             final cloneReq = await _dio.request(
                err.requestOptions.path,
                options: Options(
                    method: err.requestOptions.method,
                    headers: err.requestOptions.headers,
                ),
                data: err.requestOptions.data,
                queryParameters: err.requestOptions.queryParameters,
             );
             print('<<< Interceptor: Original request retried successfully.');
             return handler.resolve(cloneReq);
           } else {
              print('<<< Interceptor: Token refresh API call failed or returned invalid data. Status: ${response.statusCode}');
              await _performLogout();
           }
         } catch (e) {
           print('<<< Interceptor: Exception during token refresh: $e. Logging out.');
           await _performLogout();
         }
       } else {
          print('<<< Interceptor: No refresh token found. Logging out.');
          await _performLogout();
       }
    } else if (err.response?.statusCode == 403) {
        print('<<< Interceptor: Received 403 Forbidden for ${err.requestOptions.path}. Check token validity or backend permissions.');
    }

    return handler.next(err);
  }

  Future<void> _performLogout() async {
      await _storage.delete(key: StorageKeys.jwtToken);
      await _storage.delete(key: StorageKeys.refreshToken);
  }
}

class ApiService {
  late final Dio _dio;

  ApiService() {
    final baseUrl = AppConfig.baseUrl;
    print('ApiService: Initializing with base URL: $baseUrl');

    _dio = Dio(BaseOptions(
      baseUrl: baseUrl,
      connectTimeout: const Duration(seconds: 15),
      receiveTimeout: const Duration(seconds: 15),
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
      },
    ));

    _dio.interceptors.add(AuthInterceptor(const FlutterSecureStorage(), _dio));

    _dio.interceptors.add(LogInterceptor(
      requestHeader: true,
      requestBody: true,
      responseHeader: true,
      responseBody: true,
      error: true,
      logPrint: (o) => print(o.toString()),
    ));
  }

  Future<AuthResponse> register(String email, String password, String name) async {
    try {
      print('ApiService: Calling POST /auth/register');
      final response = await _dio.post('/auth/register',
        data: {
          'email': email,
          'password': password,
          'name': name,
        },
      );
      return AuthResponse.fromJson(response.data);
    } catch (e) {
      throw _handleError(e);
    }
  }

  Future<AuthResponse> login(String email, String password) async {
    try {
      print('ApiService: Calling POST /auth/login');
      final response = await _dio.post('/auth/login',
        data: {
          'email': email,
          'password': password,
        },
      );
      return AuthResponse.fromJson(response.data);
    } catch (e) {
      throw _handleError(e);
    }
  }

  Future<void> logout() async {
    try {
      print('ApiService: Calling POST /auth/logout');
      await _dio.post('/auth/logout');
    } catch (e) {
      print('ApiService: Error during backend logout call: $e');
    }
  }

  Future<UserProfile> getUserProfile() async {
    try {
      print('ApiService: Calling GET /profile');
      final response = await _dio.get('/profile');
      return UserProfile.fromJson(response.data);
    } catch (e) {
      throw _handleError(e);
    }
  }

  Future<UserProfile> updateUserProfile(Map<String, dynamic> data) async {
     try {
       print('ApiService: Calling PUT /profile');
       final response = await _dio.put('/profile', data: data);
       return UserProfile.fromJson(response.data);
     } catch (e) {
       throw _handleError(e);
     }
   }

  Future<List<Category>> getCategories() async {
    try {
      print('ApiService: Calling GET /categories');
      final response = await _dio.get('/categories');
      List<dynamic> data = response.data;
      return data.map((json) => Category.fromJson(json)).toList();
    } catch (e) {
      throw _handleError(e);
    }
  }

  Future<Category> createCategory(Category category) async {
    try {
      print('ApiService: Calling POST /categories');
      final response = await _dio.post('/categories',
        data: jsonEncode(category.toJson()),
      );
      return Category.fromJson(response.data);
    } catch (e) { throw _handleError(e); }
  }

  Future<Category> updateCategory(Category category) async {
    try {
      print('ApiService: Calling PUT /categories/${category.id}');
      final response = await _dio.put('/categories/${category.id}',
        data: jsonEncode(category.toJson()),
      );
      return Category.fromJson(response.data);
    } catch (e) { throw _handleError(e); }
  }

  Future<void> deleteCategory(int id) async {
    try {
      print('ApiService: Calling DELETE /categories/$id');
      await _dio.delete('/categories/$id');
    } catch (e) {
      throw _handleError(e);
    }
  }

  Future<List<Goal>> getGoals() async {
    try {
      print('ApiService: Calling GET /goals');
      final response = await _dio.get('/goals');
      print('ApiService: Goals response: ${response.data}');
      
      final List<dynamic> data = response.data;
      return data.map((item) => Goal.fromJson(item)).toList();
    } catch (e) {
      print('ApiService: Error getting goals: $e');
      throw _handleError(e);
    }
  }

  Future<Goal> createGoal(Goal goal) async {
    try {
      final Map<String, dynamic> data = {
        'name': goal.name,
        'target_amount': goal.targetAmount,
      };

      
      if (goal.targetDate != null) {
        data['target_date'] = DateFormat('yyyy-MM-dd').format(goal.targetDate!);
      }
      
      if (goal.priority != null && goal.priority!.isNotEmpty) {
        data['priority'] = goal.priority;
      }
      
      print('ApiService: Calling POST /goals with data: $data');
      final response = await _dio.post('/goals', data: data);
      print('ApiService: Create goal response: ${response.data}');

      if (response.data is Map<String, dynamic> && response.data.containsKey('goal')) {
        return Goal.fromJson(response.data['goal']);
      } else {
        return Goal.fromJson(response.data);
      }
    } catch (e) {
      print('ApiService: Error creating goal: $e');
      throw _handleError(e);
    }
  }

  Future<Goal> updateGoal(Goal goal) async {
    try {
      final Map<String, dynamic> data = {
        'name': goal.name,
        'target_amount': goal.targetAmount,
        'current_amount': goal.currentAmount,
      };
      
      if (goal.targetDate != null) {
        data['target_date'] = DateFormat('yyyy-MM-dd').format(goal.targetDate!);
      }
      
      if (goal.priority != null && goal.priority!.isNotEmpty) {
        data['priority'] = goal.priority;
      }
      
      print('ApiService: Calling PUT /goals/${goal.id} with data: $data');
      final response = await _dio.put('/goals/${goal.id}', data: data);
      print('ApiService: Update goal response: ${response.data}');
      
      if (response.data is Map<String, dynamic> && response.data.containsKey('goal')) {
        return Goal.fromJson(response.data['goal']);
      } else {
        return Goal.fromJson(response.data);
      }
    } catch (e) {
      print('ApiService: Error updating goal: $e');
      throw _handleError(e);
    }
  }

  Future<void> deleteGoal(int id) async {
    try {
      print('ApiService: Calling DELETE /goals/$id');
      await _dio.delete('/goals/$id');
    } catch (e) {
      print('ApiService: Error deleting goal: $e');
      throw _handleError(e);
    }
  }

  Future<List<Limit>> getLimits() async {
    try {
      print('ApiService: Calling GET /limits');
      final response = await _dio.get('/limits');
      final List<dynamic> data = response.data;
      return data.map((item) => Limit.fromJson(item)).toList();
    } catch (e) {
      throw _handleError(e);
    }
  }

  Future<Limit> createLimit(Limit limit) async {
    try {
      print('ApiService: Calling POST /limits');
      final response = await _dio.post('/limits',
        data: jsonEncode(limit.toJson()),
      );
      return Limit.fromJson(response.data);
    } catch (e) { throw _handleError(e); }
  }

  Future<Limit> updateLimit(Limit limit) async {
    try {
      print('ApiService: Calling PUT /limits/${limit.id}');
      final response = await _dio.put('/limits/${limit.id}',
        data: jsonEncode(limit.toJson()),
      );
      return Limit.fromJson(response.data);
    } catch (e) { throw _handleError(e); }
  }

  Future<void> deleteLimit(int id) async {
    try {
      print('ApiService: Calling DELETE /limits/$id');
      await _dio.delete('/limits/$id');
    } catch (e) {
      throw _handleError(e);
    }
  }

  Future<List<Transaction>> getTransactions({String? dateFrom, String? dateTo, String? categoryName}) async {
    try {
      print("ApiService: Calling GET /transactions");
      
      Map<String, dynamic> queryParams = {};
      if (dateFrom != null) queryParams['dateFrom'] = dateFrom;
      if (dateTo != null) queryParams['dateTo'] = dateTo;
      if (categoryName != null) queryParams['categoryName'] = categoryName;
      
      final response = await _dio.get('/transactions', queryParameters: queryParams);
      
      List<Transaction> result = [];
      
      if (response.data is List) {
        final List<dynamic> data = response.data;
        for (var item in data) {
          try {
            result.add(Transaction.fromJson(item));
          } catch (e) {
            print("Ошибка при парсинге транзакции: $e");
          }
        }
      } else if (response.data is Map && response.data.containsKey('transactions')) {
        final List<dynamic> data = response.data['transactions'];
        for (var item in data) {
          try {
            result.add(Transaction.fromJson(item));
          } catch (e) {
            print("Ошибка при парсинге транзакции: $e");
          }
        }
      }
      
      // Сортируем транзакции по дате (новые сверху)
      result.sort((a, b) => b.date.compareTo(a.date));
      
      return result;
    } catch (e) {
      print("ApiService: Error getting transactions: $e");
      return [];
    }
  }

   Future<Transaction> addTransaction(Map<String, dynamic> data) async {
     try {
       print('ApiService: Calling POST /transactions');
       final response = await _dio.post('/transactions', data: data);
       if (response.data != null && response.data['transaction'] != null) {
          return Transaction.fromJson(response.data['transaction']);
       } else {
          print("ApiService addTransaction: Unexpected response structure: ${response.data}");
          throw Exception('Неожиданный формат ответа от сервера при добавлении транзакции');
       }
     } catch (e) {
       throw _handleError(e);
     }
   }

  Future<Transaction> updateTransaction(Transaction transaction) async {
    if (transaction.id == null) {
      throw Exception("Transaction ID is required for update.");
    }
    try {
      print("ApiService: Calling PUT /transactions/${transaction.id}");
      // Используем toJson(), который теперь включает amount_to_goal если нужно
      final data = transaction.toJson();
      print("ApiService: Sending data for update: $data"); // Логируем отправляемые данные

      final response = await _dio.put('/transactions/${transaction.id}', data: data);

      if (response.data is Map<String, dynamic> && response.data.containsKey('transaction')) {
        return Transaction.fromJson(response.data['transaction']);
      } else {
        return Transaction.fromJson(response.data);
      }
    } catch (e) {
      print("ApiService: Error updating transaction: $e");
      throw _handleError(e);
    }
  }

  Future<void> deleteTransaction(int id) async {
    try {
      print("ApiService: Calling DELETE /transactions/$id");
      await _dio.delete('/transactions/$id');
    } catch (e) {
      throw _handleError(e);
    }
  }

  Future<String> sendMessageToAI(String message) async {
    try {
      print('ApiService: Calling POST /ai/chat');
      final response = await _dio.post('/ai/chat',
        data: { 'message': message },
      );
      if (response.data != null && response.data['response'] != null) {
         return response.data['response'];
      } else {
         throw Exception('Неожиданный формат ответа от AI чата');
      }
    } catch (e) { throw _handleError(e); }
  }

  Future<Map<String, dynamic>> getStatistics({String? period}) async {
    try {
      print('ApiService: Calling GET /statistics');
      final Map<String, dynamic> queryParams = {};
      if (period != null) {
        queryParams['period'] = period;
      }
      
      final response = await _dio.get('/statistics', queryParameters: queryParams);
      return response.data;
    } catch (e) {
      print("Ошибка при получении статистики: $e");
      // Возвращаем пустой объект вместо выбрасывания исключения
      return {
        'totalIncome': 0.0,
        'totalExpense': 0.0,
        'balance': 0.0,
        'categoryBreakdown': [],
        'dailyExpenses': {},
        'monthlyExpenses': {}
      };
    }
  }

  Future<void> changePassword(String currentPassword, String newPassword) async {
    try {
      print('ApiService: Calling POST /auth/change-password');
      await _dio.post('/auth/change-password',
        data: {
          'currentPassword': currentPassword,
          'newPassword': newPassword,
        },
      );
    } catch (e) { throw _handleError(e); }
  }

  Future<Transaction> createTransaction(Transaction transaction) async {
    try {
      print("ApiService: Calling POST /transactions");
      // Используем toJson(), который теперь включает amount_to_goal если нужно
      final data = transaction.toJson();
      print("ApiService: Sending data for create: $data"); // Логируем отправляемые данные

      final response = await _dio.post('/transactions', data: data);

      if (response.data is Map<String, dynamic> && response.data.containsKey('transaction')) {
        return Transaction.fromJson(response.data['transaction']);
      } else {
        return Transaction.fromJson(response.data);
      }
    } catch (e) {
      print("ApiService: Error creating transaction: $e");
      throw _handleError(e);
    }
  }

  Exception _handleError(dynamic error) {
    print("ApiService handleError: Type: ${error.runtimeType}, Error: $error");
    if (error is DioException) {
      print("ApiService handleError: DioException Response: ${error.response?.data}");
      print("ApiService handleError: DioException Type: ${error.type}");
      if (error.response != null) {
        final statusCode = error.response!.statusCode;
        final data = error.response!.data;
        String message = "Ошибка сервера";

        if (data is Map<String, dynamic>) {
           if (data.containsKey('message') && data['message'] != null) {
              message = data['message'].toString();
           } else if (data.containsKey('error') && data['error'] != null) {
              message = data['error'].toString();
           } else if (data.containsKey('detail') && data['detail'] != null) {
              message = data['detail'].toString();
           }
        } else if (data is String && data.isNotEmpty) {
           message = data;
        }

        switch (statusCode) {
          case 400: return Exception('Неверный запрос: $message');
          case 401: return Exception('Ошибка авторизации: $message');
          case 403: return Exception('Доступ запрещен: $message');
          case 404: return Exception('Не найдено: $message');
          case 409: return Exception('Конфликт: $message');
          case 500: return Exception('Внутренняя ошибка сервера: $message');
          default: return Exception('Ошибка HTTP $statusCode: $message');
        }
      } else {
        switch (error.type) {
          case DioExceptionType.connectionTimeout:
          case DioExceptionType.sendTimeout:
          case DioExceptionType.receiveTimeout:
            return Exception('Превышено время ожидания сети');
          case DioExceptionType.connectionError:
             return Exception('Ошибка сетевого соединения');
          default:
            return Exception('Сетевая ошибка: ${error.message}');
        }
      }
    }
    return Exception('Неизвестная ошибка: $error');
  }
} 