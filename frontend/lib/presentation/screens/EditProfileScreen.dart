import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:image_picker/image_picker.dart';
import 'dart:io';
import '../theme/app_theme.dart';
import '../../providers/profile_provider.dart';
import 'package:moneyguard/models/user_profile.dart';

class EditProfileScreen extends StatefulWidget {
  const EditProfileScreen({super.key});

  @override
  State<EditProfileScreen> createState() => _EditProfileScreenState();
}

class _EditProfileScreenState extends State<EditProfileScreen> {
  final ImagePicker _picker = ImagePicker();
  File? _selectedImage;
  final TextEditingController _nameController = TextEditingController();
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();

  @override
  void initState() {
    super.initState();
    // Загружаем текущие данные пользователя
    final profileProvider = Provider.of<ProfileProvider>(context, listen: false);
    if (profileProvider.profile != null) {
      _nameController.text = profileProvider.profile!.name;
    }
  }

  Future<void> _pickImage() async {
    final XFile? pickedFile = await _picker.pickImage(source: ImageSource.gallery);
    if (pickedFile != null) {
      setState(() {
        _selectedImage = File(pickedFile.path);
      });
    }
  }

  Future<void> _saveProfile() async {
    if (_formKey.currentState!.validate()) {
      final profileProvider = Provider.of<ProfileProvider>(context, listen: false);
      final currentProfile = profileProvider.profile;

      if (currentProfile == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Не удалось загрузить текущий профиль')),
        );
        return;
      }

      final updatedProfileData = UserProfile(
        id: currentProfile.id,
        name: _nameController.text,
        email: currentProfile.email,
        profileImage: _selectedImage?.path,
        aiAccessEnabled: currentProfile.aiAccessEnabled,
      );

      final success = await profileProvider.updateProfile(updatedProfileData);

      if (success && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Профиль успешно обновлен')),
        );
        Navigator.of(context).pop();
      } else if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Ошибка обновления профиля: ${profileProvider.error ?? 'Неизвестная ошибка'}')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final profileProvider = Provider.of<ProfileProvider>(context);
    
    return Scaffold(
      appBar: AppBar(
        title: Text('Редактировать профиль'),
      ),
      body: profileProvider.isLoading
          ? Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  // Аватар пользователя
                  GestureDetector(
                    onTap: _pickImage,
                    child: Stack(
                      children: [
                        CircleAvatar(
                          radius: 50,
                          backgroundColor: AppTheme.secondaryCardColor,
                          backgroundImage: _selectedImage != null
                              ? FileImage(_selectedImage!) as ImageProvider<Object>
                              : profileProvider.profile?.profileImage != null
                                  ? NetworkImage(profileProvider.profile!.profileImage!) as ImageProvider<Object>
                                  : null,
                          child: _selectedImage == null &&
                                  profileProvider.profile?.profileImage == null
                              ? Icon(Icons.person, size: 50, color: AppTheme.textColor)
                              : null,
                        ),
                        Positioned(
                          bottom: 0,
                          right: 0,
                          child: Container(
                            padding: EdgeInsets.all(4),
                            decoration: BoxDecoration(
                              color: AppTheme.primaryColor,
                              shape: BoxShape.circle,
                            ),
                            child: Icon(
                              Icons.camera_alt,
                              color: Colors.black,
                              size: 20,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),

                  SizedBox(height: 32),

                  // Поле для имени
                  TextField(
                    controller: _nameController,
                    decoration: AppTheme.inputDecoration('Имя', Icons.person),
                    style: TextStyle(color: AppTheme.textColor),
                  ),

                  SizedBox(height: 32),

                  // Кнопка сохранения
                  ElevatedButton(
                    onPressed: profileProvider.isLoading ? null : _saveProfile,
                    child: Text('Сохранить'),
                  ),
                ],
              ),
            ),
    );
  }

  @override
  void dispose() {
    _nameController.dispose();
    super.dispose();
  }
}