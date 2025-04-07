package src.main.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import src.main.dto.auth.AuthResponse;
import src.main.dto.auth.LoginRequest;
import src.main.dto.auth.RegisterRequest;
import src.main.exception.BusinessException;
import src.main.exception.ConflictException;
import src.main.exception.ResourceNotFoundException;
import src.main.model.User;
import src.main.service.AuthService;
import src.main.security.JwtService;
import src.main.repository.UserRepository;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        log.debug("REST запрос на регистрацию пользователя: {}", request.getEmail());
        try {
            // Проверка существования пользователя
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ConflictException("Пользователь с таким email уже существует");
            }
            
            // Создание пользователя
            User user = User.builder()
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .name(request.getName())
                    .createdAt(LocalDateTime.now())
                    .aiAccessEnabled(false)
                    .build();
            
            userRepository.save(user);
            log.info("Пользователь успешно зарегистрирован: {}", request.getEmail());
            
            // Генерация токена
            String token = jwtService.generateToken(user);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.builder()
                    .token(token)
                    .email(user.getEmail())
                    .name(user.getName())
                    .ai_access_enabled(user.isAiAccessEnabled())
                    .build());
        } catch (ConflictException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при регистрации пользователя: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при регистрации пользователя", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        log.debug("REST запрос на аутентификацию пользователя: {}", request.getEmail());
        try {
            // Аутентификация пользователя
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            
            // Получение пользователя
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
            
            // Генерация токена
            String token = jwtService.generateToken(user);
            
            return ResponseEntity.ok(AuthResponse.builder()
                    .token(token)
                    .email(user.getEmail())
                    .name(user.getName())
                    .ai_access_enabled(user.isAiAccessEnabled())
                    .build());
        } catch (BadCredentialsException e) {
            log.warn("Неверные учетные данные для пользователя: {}", request.getEmail());
            throw new BusinessException("Неверный email или пароль", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.error("Ошибка при аутентификации пользователя: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при аутентификации пользователя", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        log.debug("REST запрос на выход пользователя");
        try {
            authService.logout();
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Ошибка при выходе пользователя: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при выходе пользователя", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 