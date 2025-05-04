package src.main.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import src.main.model.*;
import src.test.utils.TestDataUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static src.test.utils.TestDataUtils.*;

@DataJpaTest
@ActiveProfiles("test")
public class TransactionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    public void whenFindByUser_thenReturnTransactions() {
        // given
        User user = entityManager.persist(createTestUser("user1@example.com"));
        Category category = entityManager.persist(createTestCategory("Food"));
        Transaction transaction = entityManager.persist(
                createTestTransaction(user, category, BigDecimal.valueOf(100))
        );
        entityManager.flush();

        // when
        List<Transaction> transactions = transactionRepository.findByUserOrderByDateDesc(user);

        // then
        assertAll(
                () -> assertFalse(transactions.isEmpty(), "Transactions list should not be empty"),
                () -> assertEquals(transaction.getAmount(), transactions.get(0).getAmount(), "Amounts should match"),
                () -> assertEquals(user.getId(), transactions.get(0).getUser().getId(), "User IDs should match"),
                () -> assertNotNull(transactions.get(0).getDate(), "Date should not be null")
        );
    }

    @Test
    public void whenFindByIdAndUser_thenReturnTransaction() {
        // given
        User user = entityManager.persist(createTestUser("user2@example.com"));
        Category category = entityManager.persist(createTestCategory("Transport"));
        Transaction transaction = entityManager.persist(
                createTestTransaction(user, category, BigDecimal.valueOf(50))
        );
        entityManager.flush();

        // when
        Optional<Transaction> found = transactionRepository.findByIdAndUser(transaction.getId(), user);

        // then
        assertAll(
                () -> assertTrue(found.isPresent(), "Transaction should be found"),
                () -> assertEquals(transaction.getAmount(), found.get().getAmount(), "Amounts should match"),
                () -> assertEquals(category.getId(), found.get().getCategory().getId(), "Category IDs should match")
        );
    }

    @Test
    public void whenFindByIdWithWrongUser_thenReturnEmpty() {
        // given
        User user1 = entityManager.persist(createTestUser("user1@example.com"));
        User user2 = entityManager.persist(createTestUser("user2@example.com"));
        Category category = entityManager.persist(createTestCategory("Food"));
        Transaction transaction = entityManager.persist(
                createTestTransaction(user1, category, BigDecimal.valueOf(100))
        );
        entityManager.flush();

        // when
        Optional<Transaction> found = transactionRepository.findByIdAndUser(transaction.getId(), user2);

        // then
        assertFalse(found.isPresent(), "Transaction should not be found for wrong user");
    }

    @Test
    public void whenFindByCategory_thenReturnTransactions() {
        // given
        User user = entityManager.persist(createTestUser("user3@example.com"));
        Category category = entityManager.persist(createTestCategory("Entertainment"));
        Transaction transaction = entityManager.persist(
                createTestTransaction(user, category, BigDecimal.valueOf(75))
        );
        entityManager.flush();

        // when
        List<Transaction> transactions = transactionRepository.findByUserAndCategoryIdOrderByDateDesc(user, category.getId());

        // then
        assertAll(
                () -> assertFalse(transactions.isEmpty(), "Transactions list should not be empty"),
                () -> assertEquals(category.getId(), transactions.get(0).getCategory().getId(), "Category IDs should match")
        );
    }
}