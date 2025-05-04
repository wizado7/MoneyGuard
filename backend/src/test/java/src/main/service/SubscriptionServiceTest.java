package src.main.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import src.main.dto.subscription.SubscriptionResponse;
import src.main.model.SubscriptionType;
import src.main.model.User;
import src.main.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("test@example.com");
        testUser.setAiAccessEnabled(false);

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("test@example.com");
    }

    @Test
    void getSubscription_ShouldReturnFreeSubscription() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        SubscriptionResponse result = subscriptionService.getSubscription();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getFeatures().size());
    }

    @Test
    void enableAIAccess_ShouldEnablePremiumFeatures() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        SubscriptionResponse result = subscriptionService.enableAIAccess();

        // Assert
        assertNotNull(result);
        assertEquals(6, result.getFeatures().size());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void disableAIAccess_ShouldDisablePremiumFeatures() {
        // Arrange
        testUser.setAiAccessEnabled(true);
        testUser.setSubscriptionType(SubscriptionType.PREMIUM);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        subscriptionService.disableAIAccess();

        // Assert
        assertFalse(testUser.isAiAccessEnabled());
        assertEquals(SubscriptionType.FREE, testUser.getSubscriptionType());
        verify(userRepository, times(1)).save(any(User.class));
    }
}