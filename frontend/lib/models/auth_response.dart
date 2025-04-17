import 'package:json_annotation/json_annotation.dart';

part 'auth_response.g.dart';

@JsonSerializable()
class AuthResponse {
  final String token;
  
  @JsonKey(name: 'refresh_token', defaultValue: '')
  final String refreshToken;
  
  @JsonKey(name: 'user_id', defaultValue: '')
  final String userId;
  
  final String email;
  final String name;
  
  @JsonKey(name: 'ai_access_enabled')
  final bool aiAccessEnabled;

  final int? id;

  AuthResponse({
    required this.token,
    this.refreshToken = '',
    this.userId = '',
    required this.email,
    required this.name,
    required this.aiAccessEnabled,
    this.id,
  });

  factory AuthResponse.fromJson(Map<String, dynamic> json) =>
      _$AuthResponseFromJson(json);

  Map<String, dynamic> toJson() => _$AuthResponseToJson(this);
} 