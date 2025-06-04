import 'package:json_annotation/json_annotation.dart';

part 'user_profile.g.dart';

@JsonSerializable()
class UserProfile {
  final int id;
  final String name;
  final String email;
  @JsonKey(name: 'profileImage')
  final String? profileImage;
  @JsonKey(name: 'aiAccessEnabled')
  final bool aiAccessEnabled;
  @JsonKey(name: 'subscriptionType')
  final String? subscriptionType;
  @JsonKey(name: 'subscriptionExpiry')
  final String? subscriptionExpiry;

  UserProfile({
    required this.id,
    required this.name,
    required this.email,
    this.profileImage,
    required this.aiAccessEnabled,
    this.subscriptionType,
    this.subscriptionExpiry,
  });

  factory UserProfile.fromJson(Map<String, dynamic> json) =>
      _$UserProfileFromJson(json);

  Map<String, dynamic> toJson() => _$UserProfileToJson(this);

  String get fullName => name;
} 