import 'package:flutter/material.dart';

class AppTheme {
  // Основные цвета
  static const Color primaryColor = Colors.white;
  static const Color backgroundColor = Colors.black;
  static const Color cardColor = Color(0xFF1E1E1E);
  static const Color secondaryCardColor = Color(0xFF2C2C2C);
  static const Color textColor = Colors.white;
  static const Color secondaryTextColor = Color(0xFFAAAAAA);
  static const Color accentColor = Colors.white;
  
  // Тема приложения
  static ThemeData get darkTheme {
    return ThemeData(
      useMaterial3: true,
      colorScheme: ColorScheme.dark(
        primary: primaryColor,
        background: backgroundColor,
        surface: cardColor,
        onPrimary: Colors.black,
        onBackground: textColor,
        onSurface: textColor,
      ),
      scaffoldBackgroundColor: backgroundColor,
      fontFamily: 'PlusJakartaSans',
      
      // Стиль AppBar
      appBarTheme: AppBarTheme(
        backgroundColor: backgroundColor,
        elevation: 0,
        centerTitle: true,
        titleTextStyle: TextStyle(
          color: textColor,
          fontSize: 20,
          fontWeight: FontWeight.bold,
          fontFamily: 'PlusJakartaSans',
        ),
        iconTheme: IconThemeData(color: textColor),
      ),
      
      // Стиль кнопок
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: primaryColor,
          foregroundColor: Colors.black,
          minimumSize: const Size(double.infinity, 56),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8),
          ),
          padding: EdgeInsets.symmetric(vertical: 16),
          textStyle: const TextStyle(
            fontFamily: 'PlusJakartaSans',
            fontSize: 18,
            fontWeight: FontWeight.w600,
          ),
        ),
      ),
      
      // Стиль текстовых полей
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: secondaryCardColor,
        hintStyle: TextStyle(color: secondaryTextColor),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: BorderSide.none,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: BorderSide.none,
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: BorderSide(color: primaryColor),
        ),
        contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        prefixIconColor: textColor,
        suffixIconColor: textColor,
      ),
      
      // Стиль текста
      textTheme: const TextTheme(
        displayLarge: TextStyle(
          fontFamily: 'PlusJakartaSans',
          fontSize: 32,
          fontWeight: FontWeight.bold,
          color: textColor,
        ),
        displayMedium: TextStyle(
          fontFamily: 'PlusJakartaSans',
          fontSize: 28,
          fontWeight: FontWeight.bold,
          color: textColor,
        ),
        titleLarge: TextStyle(
          fontFamily: 'PlusJakartaSans',
          fontSize: 22,
          fontWeight: FontWeight.w600,
          color: textColor,
        ),
        titleMedium: TextStyle(
          fontFamily: 'PlusJakartaSans',
          fontSize: 18,
          fontWeight: FontWeight.w600,
          color: textColor,
        ),
        bodyLarge: TextStyle(
          fontFamily: 'PlusJakartaSans',
          fontSize: 16,
          color: textColor,
        ),
        bodyMedium: TextStyle(
          fontFamily: 'PlusJakartaSans',
          fontSize: 14,
          color: textColor,
        ),
        labelLarge: TextStyle(
          fontFamily: 'PlusJakartaSans',
          fontSize: 16,
          fontWeight: FontWeight.w600,
          color: textColor,
        ),
      ),
      
      // Стиль карточек
      cardTheme: CardTheme(
        color: cardColor,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
      ),
      
      // Стиль нижней навигации
      bottomNavigationBarTheme: BottomNavigationBarThemeData(
        backgroundColor: backgroundColor,
        selectedItemColor: primaryColor,
        unselectedItemColor: secondaryTextColor,
        type: BottomNavigationBarType.fixed,
        elevation: 0,
      ),
    );
  }
  
  // Стили для виджетов
  static BoxDecoration get cardDecoration {
    return BoxDecoration(
      color: cardColor,
      borderRadius: BorderRadius.circular(16),
    );
  }
  
  static BoxDecoration get secondaryCardDecoration {
    return BoxDecoration(
      color: secondaryCardColor,
      borderRadius: BorderRadius.circular(16),
    );
  }
  
  static InputDecoration inputDecoration(String hint, IconData icon) {
    return InputDecoration(
      hintText: hint,
      prefixIcon: Icon(icon),
      filled: true,
      fillColor: secondaryCardColor,
      hintStyle: TextStyle(color: secondaryTextColor),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: BorderSide.none,
      ),
    );
  }
} 