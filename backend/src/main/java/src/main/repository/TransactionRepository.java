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
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    List<Transaction> findByUserOrderByDateDesc(User user);
    
    List<Transaction> findByUserAndDateBetweenOrderByDateDesc(User user, LocalDate dateFrom, LocalDate dateTo);
    
    List<Transaction> findByUserAndCategoryIdOrderByDateDesc(User user, Long categoryId);
    
    List<Transaction> findByUserAndGoal(User user, Goal goal);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.category = :category AND t.date BETWEEN :dateFrom AND :dateTo AND t.amount < 0")
    BigDecimal sumByUserAndCategoryAndPeriod(@Param("user") User user, @Param("category") Category category, @Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo);
} 