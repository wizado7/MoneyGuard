package src.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import src.main.model.Category;
import src.main.model.Goal;
import src.main.model.Transaction;
import src.main.model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findByUserOrderByDateDesc(User user);

    Optional<Transaction> findByIdAndUser(Integer id, User user);

    List<Transaction> findByUserAndDateBetweenOrderByDateDesc(User user, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByUserAndCategoryIdOrderByDateDesc(User user, Integer categoryId);

    List<Transaction> findByUserAndCategoryIdAndDateBetweenOrderByDateDesc(User user, Integer categoryId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.category = :category AND t.date >= :startDate AND t.amount < 0")
    BigDecimal sumAmountByUserAndCategoryAndDateAfter(
            @Param("user") User user,
            @Param("category") Category category,
            @Param("startDate") LocalDateTime startDate
    );

    boolean existsByCategoryId(Integer categoryId);

    List<Transaction> findByUserAndGoal(User user, Goal goal);

    Optional<Transaction> findByIdAndUserId(Integer id, Integer userId);

}