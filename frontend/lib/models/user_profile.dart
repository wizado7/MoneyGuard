import 'package:json_annotation/json_annotation.dart';

part 'user_profile.g.dart';

@JsonSerializable()
class UserProfile {
  final String id;
  final String name;
  final String email;
  @JsonKey(name: 'profile_image')
  final String? profileImage;
  @JsonKey(name: 'ai_access_enabled')
  final bool aiAccessEnabled;
  @JsonKey(name: 'subscription_type')
  final String? subscriptionType;
  @JsonKey(name: 'subscription_expiry')
  final DateTime? subscriptionExpiry;
  @JsonKey(name: 'created_at')
  final DateTime? createdAt;
  @JsonKey(name: 'updated_at')
  final DateTime? updatedAt;

  UserProfile({
    required this.id,
    required this.name,
    required this.email,
    this.profileImage,
    required this.aiAccessEnabled,
    this.subscriptionType,
    this.subscriptionExpiry,
    this.createdAt,
    this.updatedAt,
  });

  factory UserProfile.fromJson(Map<String, dynamic> json) =>
      _$UserProfileFromJson(json);

  Map<String, dynamic> toJson() => _$UserProfileToJson(this);

  String get fullName => '$name';
} 