import 'package:flutter/material.dart';

class CircleIndicator extends StatelessWidget {
  final bool isActive;

  const CircleIndicator({super.key, required this.isActive});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 10,
      height: 10,
      margin: EdgeInsets.symmetric(horizontal: 4),
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: isActive ? Colors.white : Colors.grey, // لون الدائرة بناءً على الحالة
      ),
    );
  }
}