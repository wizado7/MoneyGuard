package src.main.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import src.main.dto.subscription.SubscriptionRequest;
import src.main.dto.subscription.SubscriptionResponse;
import src.main.service.SubscriptionService;
import src.main.exception.BusinessException;
import src.main.exception.InvalidDataException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/subscription")
@RequiredArgsConstructor
@Validated
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public ResponseEntity<SubscriptionResponse> getSubscription() {
        log.debug("REST запрос на получение информации о подписке");
        try {
            return ResponseEntity.ok(subscriptionService.getSubscription());
        } catch (Exception e) {
            log.error("Ошибка при получении информации о подписке: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при получении информации о подписке", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> enablePremium() {
        log.debug("REST запрос на активацию PREMIUM подписки");
        try {
            return ResponseEntity.ok(subscriptionService.enableAIAccess());
        } catch (Exception e) {
            log.error("Ошибка при активации PREMIUM подписки: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при активации PREMIUM подписки", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> disablePremium() {
        log.debug("REST запрос на деактивацию PREMIUM подписки");
        try {
            subscriptionService.disableAIAccess();
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Ошибка при деактивации PREMIUM подписки: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при деактивации PREMIUM подписки", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 