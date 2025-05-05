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
import src.main.dto.goal.GoalRequest;
import src.main.dto.goal.GoalResponse;
import src.main.model.*;
import src.main.repository.GoalRepository;
import src.main.repository.UserRepository;
import src.main.repository.TransactionRepository;
import src.main.exception.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private GoalService goalService;

    private User testUser;
    private Goal testGoal;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("test@example.com");

        testGoal = new Goal();
        testGoal.setId(1);
        testGoal.setName("New Car");
        testGoal.setTargetAmount(BigDecimal.valueOf(10000));
        testGoal.setCurrentAmount(BigDecimal.ZERO);
        testGoal.setTargetDate(LocalDate.now().plusMonths(12));
        testGoal.setPriority(GoalPriority.MEDIUM);
        testGoal.setUser(testUser);

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("test@example.com");
    }

    @Test
    void getAllGoals_ShouldReturnUserGoals() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(goalRepository.findByUserId(1)).thenReturn(List.of(testGoal));

        // Act
        List<GoalResponse> result = goalService.getAllGoals();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("New Car", result.get(0).getName());
    }

    @Test
    void createGoal_ShouldCreateNewGoal() {
        // Arrange
        GoalRequest request = new GoalRequest();
        request.setName("New Car");
        request.setTarget_amount(BigDecimal.valueOf(10000));
        request.setTarget_date(LocalDate.now().plusMonths(12));
        request.setPriority("MEDIUM");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(goalRepository.save(any(Goal.class))).thenReturn(testGoal);

        // Act
        GoalResponse result = goalService.createGoal(request);

        // Assert
        assertNotNull(result);
        assertEquals("New Car", result.getName());
        verify(goalRepository, times(1)).save(any(Goal.class));
    }

    @Test
    void createGoal_ShouldThrowWhenInvalidData() {
        // Arrange
        GoalRequest request = new GoalRequest();
        request.setName("New Car");
        request.setTarget_amount(BigDecimal.valueOf(-100));
        request.setTarget_date(LocalDate.now().minusDays(1));
        request.setPriority("MEDIUM");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(InvalidDataException.class, () -> goalService.createGoal(request));
    }

    @Test
    void updateGoal_ShouldUpdateGoal() {
        // Arrange
        GoalRequest request = new GoalRequest();
        request.setName("Updated Car");
        request.setTarget_amount(BigDecimal.valueOf(12000));
        request.setCurrent_amount(BigDecimal.valueOf(2000));
        request.setTarget_date(LocalDate.now().plusMonths(10));
        request.setPriority("HIGH");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(goalRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(testGoal));
        when(goalRepository.save(any(Goal.class))).thenReturn(testGoal);

        // Act
        goalService.updateGoal(1, request);

        // Assert
        verify(goalRepository, times(1)).save(any(Goal.class));
    }

    @Test
    void deleteGoal_ShouldDeleteGoal() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(goalRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(testGoal));
        when(transactionRepository.findByUserAndGoal(testUser, testGoal)).thenReturn(List.of());

        // Act
        goalService.deleteGoal(1);

        // Assert
        verify(goalRepository, times(1)).delete(testGoal);
    }
}