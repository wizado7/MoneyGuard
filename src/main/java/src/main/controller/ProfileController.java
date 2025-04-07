package src.main.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import src.main.dto.profile.ProfileResponse;
import src.main.dto.profile.ProfileUpdateRequest;
import src.main.exception.BusinessException;
import src.main.exception.DuplicateResourceException;
import src.main.exception.ResourceNotFoundException;
import src.main.service.ProfileService;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile() {
        log.debug("REST запрос на получение профиля пользователя");
        try {
            return ResponseEntity.ok(profileService.getProfile());
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при получении профиля пользователя: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при получении профиля пользователя", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(@RequestBody @Valid ProfileUpdateRequest request) {
        log.debug("REST запрос на обновление профиля пользователя");
        try {
            return ResponseEntity.ok(profileService.updateProfile(request));
        } catch (ResourceNotFoundException | DuplicateResourceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при обновлении профиля пользователя: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при обновлении профиля пользователя", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 