package src.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import src.main.dto.profile.ProfileResponse;
import src.main.dto.profile.ProfileUpdateRequest;
import src.main.exception.DuplicateResourceException;
import src.main.exception.ResourceNotFoundException;
import src.main.model.User;
import src.main.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Cacheable(value = "userProfile", key = "#root.target.getCurrentUserEmail()")
    public ProfileResponse getProfile() {
        User currentUser = getCurrentUser();
        
        return ProfileResponse.builder()
                .id(currentUser.getId())
                .email(currentUser.getEmail())
                .name(currentUser.getName())
                .subscriptionType(currentUser.getSubscriptionType() != null ? currentUser.getSubscriptionType().name() : null)
                .subscriptionExpiry(currentUser.getSubscriptionExpiry())
                .build();
    }

    @CacheEvict(value = "userProfile", key = "#root.target.getCurrentUserEmail()")
    public ProfileResponse updateProfile(ProfileUpdateRequest request) {
        User currentUser = getCurrentUser();
        
        // Если email изменился, проверяем, что новый email не занят
        if (!request.getEmail().equals(currentUser.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Пользователь с таким email уже существует");
            }
            currentUser.setEmail(request.getEmail());
        }
        
        // Обновляем имя
        currentUser.setName(request.getName());
        
        // Если пароль не пустой, обновляем его
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            currentUser.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        userRepository.save(currentUser);
        
        return ProfileResponse.builder()
                .id(currentUser.getId())
                .email(currentUser.getEmail())
                .name(currentUser.getName())
                .subscriptionType(currentUser.getSubscriptionType() != null ? currentUser.getSubscriptionType().name() : null)
                .subscriptionExpiry(currentUser.getSubscriptionExpiry())
                .build();
    }

    // Вспомогательный метод для получения email текущего пользователя для ключа кэша
    public String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(username)
                .orElseThrow(() -> {
                    log.error("Пользователь не найден: {}", username);
                    return new UsernameNotFoundException("Пользователь " + username + " не найден");
                });
    }

    private ProfileResponse mapUserToProfileResponse(User user) {
        if (user == null) return null;
        return ProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .aiAccessEnabled(user.isAiAccessEnabled())
                .subscriptionType(user.getSubscriptionType() != null ? user.getSubscriptionType().name() : null)
                .subscriptionExpiry(user.getSubscriptionExpiry())
                .build();
    }
} 