package src.main.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import src.main.dto.limit.LimitRequest;
import src.main.dto.limit.LimitResponse;
import src.main.model.*;
import src.main.repository.CategoryRepository;
import src.main.repository.LimitRepository;
import src.main.repository.TransactionRepository;
import src.main.exception.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LimitServiceTest {

    @Mock
    private LimitRepository limitRepository;

    @Mock
    private ProfileService profileService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private LimitService limitService;

    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("test@example.com");

        testCategory = new Category();
        testCategory.setId(1);
        testCategory.setName("Food");
    }

    @Test
    void getLimits_ShouldReturnListOfLimits() {
        // Arrange
        Limit limit = new Limit();
        limit.setId(1);
        limit.setAmount(BigDecimal.valueOf(1000));
        limit.setPeriod(LimitPeriod.MONTHLY);
        limit.setUser(testUser);
        limit.setCategory(testCategory);

        when(profileService.getCurrentUser()).thenReturn(testUser);
        when(limitRepository.findByUser(testUser)).thenReturn(List.of(limit));
        when(transactionRepository.sumAmountByUserAndCategoryAndDateAfter(
                any(User.class), any(Category.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(500));

        // Act
        List<LimitResponse> result = limitService.getLimits();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
        assertEquals(BigDecimal.valueOf(1000), result.get(0).getAmount());
        assertEquals(BigDecimal.valueOf(500), result.get(0).getCurrentUsage());
    }

    @Test
    void createLimit_ShouldCreateNewLimit() {
        // Arrange
        LimitRequest request = new LimitRequest();
        request.setCategoryId(1);
        request.setAmount(BigDecimal.valueOf(1000));
        request.setPeriod("monthly");

        when(profileService.getCurrentUser()).thenReturn(testUser);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(testCategory));
        when(limitRepository.existsByCategory_IdAndPeriodAndUser(1, LimitPeriod.MONTHLY, testUser))
                .thenReturn(false);
        when(limitRepository.save(any(Limit.class))).thenAnswer(invocation -> {
            Limit l = invocation.getArgument(0);
            l.setId(1);
            return l;
        });
        when(transactionRepository.sumAmountByUserAndCategoryAndDateAfter(
                any(User.class), any(Category.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);

        // Act
        LimitResponse result = limitService.createLimit(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(BigDecimal.valueOf(1000), result.getAmount());
        assertEquals("MONTHLY", result.getPeriod());
        verify(limitRepository, times(1)).save(any(Limit.class));
    }

    @Test
    void createLimit_ShouldThrowWhenDuplicateLimitExists() {
        // Arrange
        LimitRequest request = new LimitRequest();
        request.setCategoryId(1);
        request.setAmount(BigDecimal.valueOf(1000));
        request.setPeriod("monthly");

        when(profileService.getCurrentUser()).thenReturn(testUser);
        when(limitRepository.existsByCategory_IdAndPeriodAndUser(1, LimitPeriod.MONTHLY, testUser))
                .thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> limitService.createLimit(request));
    }

    @Test
    void updateLimit_ShouldUpdateExistingLimit() {
        // Arrange
        Limit existingLimit = new Limit();
        existingLimit.setId(1);
        existingLimit.setAmount(BigDecimal.valueOf(1000));
        existingLimit.setPeriod(LimitPeriod.MONTHLY);
        existingLimit.setUser(testUser);
        existingLimit.setCategory(testCategory);

        LimitRequest request = new LimitRequest();
        request.setCategoryId(1);
        request.setAmount(BigDecimal.valueOf(1500));
        request.setPeriod("monthly");

        when(profileService.getCurrentUser()).thenReturn(testUser);
        when(limitRepository.findByIdAndUser(1, testUser)).thenReturn(Optional.of(existingLimit));
        when(limitRepository.save(any(Limit.class))).thenReturn(existingLimit);
        when(transactionRepository.sumAmountByUserAndCategoryAndDateAfter(
                any(User.class), any(Category.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(500));

        // Act
        LimitResponse result = limitService.updateLimit(1, request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(BigDecimal.valueOf(1500), result.getAmount());
        verify(limitRepository, times(1)).save(any(Limit.class));
    }

    @Test
    void deleteLimit_ShouldDeleteExistingLimit() {
        // Arrange
        Limit existingLimit = new Limit();
        existingLimit.setId(1);
        existingLimit.setUser(testUser);

        when(profileService.getCurrentUser()).thenReturn(testUser);
        when(limitRepository.findByIdAndUser(1, testUser)).thenReturn(Optional.of(existingLimit));

        // Act
        limitService.deleteLimit(1);

        // Assert
        verify(limitRepository, times(1)).delete(existingLimit);
    }

    @Test
    void getLimitById_ShouldReturnLimit() {
        // Arrange
        Limit existingLimit = new Limit();
        existingLimit.setId(1);
        existingLimit.setAmount(BigDecimal.valueOf(1000));
        existingLimit.setPeriod(LimitPeriod.MONTHLY);
        existingLimit.setUser(testUser);
        existingLimit.setCategory(testCategory);

        when(profileService.getCurrentUser()).thenReturn(testUser);
        when(limitRepository.findByIdAndUser(1, testUser)).thenReturn(Optional.of(existingLimit));
        when(transactionRepository.sumAmountByUserAndCategoryAndDateAfter(
                any(User.class), any(Category.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(300));

        // Act
        LimitResponse result = limitService.getLimitById(1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(BigDecimal.valueOf(1000), result.getAmount());
        assertEquals(BigDecimal.valueOf(300), result.getCurrentUsage());
    }
}