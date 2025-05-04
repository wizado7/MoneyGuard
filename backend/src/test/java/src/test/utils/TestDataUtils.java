package src.test.utils;

import src.main.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TestDataUtils {

    public static User createTestUser(String email) {
        return User.builder()
                .email(email)
                .password("password")
                .name("Test User")
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Goal createTestGoal(User user, String name) {
        return Goal.builder()
                .user(user)
                .name(name)
                .targetAmount(BigDecimal.valueOf(10000))
                .currentAmount(BigDecimal.ZERO)
                .targetDate(LocalDate.now().plusMonths(6))
                .priority(GoalPriority.MEDIUM)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Category createTestCategory(String name) {
        return Category.builder()
                .name(name)
                .isIncome(false)
                .isSystem(false)
                .build();
    }

    public static Category createChildCategory(Category parent, String name) {
        return Category.builder()
                .name(name)
                .parent(parent)
                .isIncome(false)
                .isSystem(false)
                .build();
    }

    public static Transaction createTestTransaction(User user, Category category, BigDecimal amount) {
        return Transaction.builder()
                .user(user)
                .category(category)
                .amount(amount)
                .date(LocalDate.now())
                .build();
    }

    public static Limit createTestLimit(User user, Category category, LimitPeriod period, BigDecimal amount) {
        return Limit.builder()
                .user(user)
                .category(category)
                .amount(amount)
                .period(period)
                .build();
    }

    public static Subscription createTestSubscription(User user, SubscriptionType type, LocalDate expiresAt) {
        return Subscription.builder()
                .user(user)
                .type(type)
                .expiresAt(expiresAt)
                .build();
    }
}