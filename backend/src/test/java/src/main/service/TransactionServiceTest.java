package src.main.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import src.main.dto.transaction.TransactionAIResponse;
import src.main.dto.transaction.TransactionRequest;
import src.main.dto.transaction.TransactionResponse;
import src.main.model.*;
import src.main.repository.*;
import src.main.exception.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private LimitRepository limitRepository;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Category incomeCategory;
    private Category expenseCategory;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("test@example.com");

        incomeCategory = new Category();
        incomeCategory.setId(1);
        incomeCategory.setName("Salary");
        incomeCategory.setIncome(true);

        expenseCategory = new Category();
        expenseCategory.setId(2);
        expenseCategory.setName("Food");
        expenseCategory.setIncome(false);

        lenient().when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    }

    @Test
    void createTransaction_ShouldCreateIncomeTransaction() {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAmount(BigDecimal.valueOf(1000));
        request.setCategoryId(1);
        request.setDate(LocalDate.now());
        request.setDescription("Salary");

        when(categoryRepository.findById(1)).thenReturn(Optional.of(incomeCategory));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(1);
            return t;
        });

        // Act
        TransactionAIResponse result = transactionService.createTransaction(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTransaction().getId());
        assertEquals(BigDecimal.valueOf(1000), result.getTransaction().getAmount());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void createTransaction_ShouldCreateExpenseTransaction() {
        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setAmount(BigDecimal.valueOf(100));
        request.setCategoryId(2);
        request.setDate(LocalDate.now());
        request.setDescription("Groceries");

        when(categoryRepository.findById(2)).thenReturn(Optional.of(expenseCategory));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(1);
            return t;
        });

        // Act
        TransactionAIResponse result = transactionService.createTransaction(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTransaction().getId());
        assertEquals(BigDecimal.valueOf(-100), result.getTransaction().getAmount());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void updateTransaction_ShouldUpdateTransaction() {
        // Arrange
        Transaction existingTransaction = new Transaction();
        existingTransaction.setId(1);
        existingTransaction.setAmount(BigDecimal.valueOf(-100));
        existingTransaction.setCategory(expenseCategory);
        existingTransaction.setUser(testUser);

        TransactionRequest request = new TransactionRequest();
        request.setAmount(BigDecimal.valueOf(150));
        request.setCategoryId(2);
        request.setDate(LocalDate.now());
        request.setDescription("Updated groceries");

        when(transactionRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(existingTransaction));
        when(categoryRepository.findById(2)).thenReturn(Optional.of(expenseCategory));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(existingTransaction);

        // Act
        TransactionResponse result = transactionService.updateTransaction(1, request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(BigDecimal.valueOf(-150), result.getAmount());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void deleteTransaction_ShouldDeleteTransaction() {
        // Arrange
        Transaction transaction = new Transaction();
        transaction.setId(1);
        transaction.setUser(testUser);

        when(transactionRepository.findByIdAndUserId(1, 1)).thenReturn(Optional.of(transaction));

        // Act
        transactionService.deleteTransaction(1);

        // Assert
        verify(transactionRepository, times(1)).delete(transaction);
    }
}