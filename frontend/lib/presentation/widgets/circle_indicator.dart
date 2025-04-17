import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class CircleIndicator extends StatelessWidget {
  final bool isActive;

  const CircleIndicator({super.key, required this.isActive});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: isActive ? 12 : 8,
      height: isActive ? 12 : 8,
      margin: EdgeInsets.symmetric(horizontal: 4),
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: isActive ? AppTheme.primaryColor : AppTheme.secondaryTextColor.withOpacity(0.5),
      ),
    );
  }
}