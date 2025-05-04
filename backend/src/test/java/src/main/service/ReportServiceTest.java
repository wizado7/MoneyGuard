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
import src.main.dto.report.ReportResponse;
import src.main.model.Category;
import src.main.model.Transaction;
import src.main.model.User;
import src.main.repository.TransactionRepository;
import src.main.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReportService reportService;

    private User testUser;
    private Transaction incomeTransaction;
    private Transaction expenseTransaction;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("test@example.com");

        Category incomeCategory = new Category();
        incomeCategory.setName("Salary");

        Category expenseCategory = new Category();
        expenseCategory.setName("Food");

        incomeTransaction = new Transaction();
        incomeTransaction.setAmount(BigDecimal.valueOf(1000));
        incomeTransaction.setCategory(incomeCategory);
        incomeTransaction.setUser(testUser);

        expenseTransaction = new Transaction();
        expenseTransaction.setAmount(BigDecimal.valueOf(-500));
        expenseTransaction.setCategory(expenseCategory);
        expenseTransaction.setUser(testUser);
    }

    @Test
    void getReport_ShouldGenerateMonthlyReport() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(transactionRepository.findByUserAndDateBetweenOrderByDateDesc(
                any(User.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(incomeTransaction, expenseTransaction));

        // Act
        ReportResponse result = reportService.getReport(null, null, "monthly");

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(1000), result.getIncome());
        assertEquals(BigDecimal.valueOf(500), result.getExpenses());
        assertEquals(BigDecimal.valueOf(500), result.getBalance());
    }
}