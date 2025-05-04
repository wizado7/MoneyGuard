package src.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import src.main.dto.subscription.SubscriptionRequest;
import src.main.dto.subscription.SubscriptionResponse;
import src.main.model.SubscriptionType;
import src.main.model.User;
import src.main.repository.UserRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final UserRepository userRepository;

    public SubscriptionResponse getSubscription() {
        User user = getCurrentUser();
        
        List<String> features = new ArrayList<>();
        features.add("Базовый учет доходов и расходов");
        features.add("Неограниченное количество категорий");
        features.add("Неограниченное количество транзакций");
        
        if (user.isAiAccessEnabled()) {
            features.add("Доступ к AI-рекомендациям");
            features.add("Расширенная аналитика финансов");
        }
        
        return SubscriptionResponse.builder()
                .aiAccessEnabled(user.isAiAccessEnabled())
                .subscriptionType(user.isAiAccessEnabled() ? "PREMIUM" : "FREE")
                .features(features)
                .build();
    }
    
    public SubscriptionResponse enableAIAccess() {
        User user = getCurrentUser();
        user.setAiAccessEnabled(true);
        user.setSubscriptionType(SubscriptionType.PREMIUM);
        user.setSubscriptionExpiry(LocalDate.now().plusMonths(1));
        userRepository.save(user);
        
        List<String> features = new ArrayList<>();
        features.add("Базовый учет доходов и расходов");
        features.add("Неограниченное количество категорий");
        features.add("Неограниченное количество транзакций");
        features.add("Доступ к AI-рекомендациям");
        features.add("Расширенная аналитика финансов");
        features.add("Приоритетная техническая поддержка");
        
        return SubscriptionResponse.builder()
                .aiAccessEnabled(true)
                .subscriptionType("PREMIUM")
                .features(features)
                .build();
    }
    
    public void disableAIAccess() {
        User user = getCurrentUser();
        user.setAiAccessEnabled(false);
        user.setSubscriptionType(SubscriptionType.FREE);
        user.setSubscriptionExpiry(null);
        userRepository.save(user);
    }
    
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }
} 