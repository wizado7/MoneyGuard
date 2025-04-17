import 'package:json_annotation/json_annotation.dart';
import 'package:flutter/material.dart';

part 'category.g.dart';

@JsonSerializable()
class Category {
  final int? id;
  final String name;
  
  @JsonKey(name: 'icon')
  final String? iconName;
  
  @JsonKey(name: 'color')
  final String? colorHex;
  
  @JsonKey(name: 'isIncome', defaultValue: false)
  final bool isIncome;
  
  @JsonKey(name: 'parentId')
  final int? parentId;
  
  @JsonKey(name: 'userId')
  final String? userId;

  int get iconCode => _getIconCodeFromName(iconName ?? '');

  Category({
    this.id,
    required this.name,
    this.iconName,
    this.colorHex,
    required this.isIncome,
    this.parentId,
    this.userId,
  });

  factory Category.fromJson(Map<String, dynamic> json) {
    try {
      // Проверяем наличие обязательных полей и устанавливаем значения по умолчанию
      if (json['isIncome'] == null) {
        json['isIncome'] = false;
      }
      
      return _$CategoryFromJson(json);
    } catch (e) {
      print("Ошибка при десериализации Category: $e");
      // Возвращаем базовую категорию в случае ошибки
      return Category(
        id: 0,
        name: "Неизвестная категория",
        iconName: "help_outline",
        colorHex: "#9E9E9E",
        isIncome: false,
      );
    }
  }

  Map<String, dynamic> toJson() => _$CategoryToJson(this);

  // Геттер для получения строкового представления иконки (для новых экранов)
  String get iconString => iconName ?? 'help_outline';

  // Геттер для получения строкового представления цвета (для новых экранов)
  String get colorString => colorHex ?? '#9E9E9E';

  // Геттер для получения IconData из строкового имени иконки
  IconData get icon {
    try {
      switch (iconName) {
        case 'shopping_cart': return Icons.shopping_cart;
        case 'directions_car': return Icons.directions_car;
        case 'local_play': return Icons.local_play;
        case 'local_hospital': return Icons.local_hospital;
        case 'checkroom': return Icons.checkroom;
        case 'restaurant': return Icons.restaurant;
        case 'card_giftcard': return Icons.card_giftcard;
        case 'receipt_long': return Icons.receipt_long;
        case 'school': return Icons.school;
        case 'flight_takeoff': return Icons.flight_takeoff;
        case 'home': return Icons.home;
        case 'help_outline': return Icons.help_outline;
        case 'account_balance_wallet': return Icons.account_balance_wallet;
        case 'work': return Icons.work;
        case 'trending_up': return Icons.trending_up;
        case 'movie': return Icons.movie;
        case 'favorite': return Icons.favorite;
        case 'shopping_bag': return Icons.shopping_bag;
        case 'attach_money': return Icons.attach_money;
        case 'account_balance': return Icons.account_balance;
        case 'phone': return Icons.phone;
        case 'update': return Icons.update;
        default: return Icons.category;
      }
    } catch (e) {
      return Icons.error;
    }
  }

  // Геттер для получения Color из HEX-строки
  Color get color {
    try {
      return Color(int.parse(colorHex?.replaceAll('#', '0xFF') ?? '0xFF9E9E9E'));
    } catch (e) {
      return Colors.grey;
    }
  }

  // Метод для получения кода иконки из имени
  int _getIconCodeFromName(String iconName) {
    switch (iconName) {
      case 'shopping_cart':
        return Icons.shopping_cart.codePoint;
      case 'directions_car':
        return Icons.directions_car.codePoint;
      case 'movie':
        return Icons.movie.codePoint;
      case 'home':
        return Icons.home.codePoint;
      case 'favorite':
        return Icons.favorite.codePoint;
      case 'attach_money':
        return Icons.attach_money.codePoint;
      case 'card_giftcard':
        return Icons.card_giftcard.codePoint;
      case 'trending_up':
        return Icons.trending_up.codePoint;
      case 'local_play':
        return Icons.local_play.codePoint;
      case 'local_hospital':
        return Icons.local_hospital.codePoint;
      case 'checkroom':
        return Icons.checkroom.codePoint;
      case 'receipt_long':
        return Icons.receipt_long.codePoint;
      case 'flight_takeoff':
        return Icons.flight_takeoff.codePoint;
      case 'help_outline':
        return Icons.help_outline.codePoint;
      case 'account_balance_wallet':
        return Icons.account_balance_wallet.codePoint;
      case 'work':
        return Icons.work.codePoint;
      default:
        return Icons.category.codePoint; // Значение по умолчанию
    }
  }
} 