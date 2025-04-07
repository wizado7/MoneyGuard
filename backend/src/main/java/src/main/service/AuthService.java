package src.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import src.main.dto.auth.AuthResponse;
import src.main.dto.auth.LoginRequest;
import src.main.dto.auth.RegisterRequest;
import src.main.dto.profile.ProfileResponse;
import src.main.model.User;
import src.main.repository.UserRepository;
import src.main.security.JwtService;
import src.main.exception.DuplicateResourceException;
import src.main.exception.ResourceNotFoundException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        log.debug("Регистрация нового пользователя: {}", request.getEmail());
        
        // Проверяем, что пользователь с таким email не существует
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Пользователь с email {} уже существует", request.getEmail());
            throw new DuplicateResourceException("Пользователь с таким email уже существует");
        }
        
        // Создаем нового пользователя
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .createdAt(LocalDateTime.now())
                .build();
        
        userRepository.save(user);
        log.info("Пользователь успешно зарегистрирован: {}", user.getEmail());
        
        // Генерируем JWT токен
        String token = jwtService.generateToken(user);
        
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .name(user.getName())
                .ai_access_enabled(user.isAiAccessEnabled())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        log.debug("Попытка входа пользователя: {}", request.getEmail());
        
        // Аутентифицируем пользователя
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        
        // Устанавливаем аутентификацию в контекст
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Получаем пользователя
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Пользователь с email {} не найден", request.getEmail());
                    return new ResourceNotFoundException("Пользователь не найден");
                });
        
        // Генерируем JWT токен
        String token = jwtService.generateToken(user);
        log.info("Пользователь успешно вошел: {}", user.getEmail());
        
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .name(user.getName())
                .ai_access_enabled(user.isAiAccessEnabled())
                .build();
    }

    public void logout() {
        log.debug("Выход пользователя");
        SecurityContextHolder.clearContext();
        log.info("Пользователь успешно вышел");
    }
} 