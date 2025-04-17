package src.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import src.main.model.Category;
import src.main.model.Limit;
import src.main.model.User;
import src.main.model.LimitPeriod;

import java.util.List;
import java.util.Optional;

@Repository
public interface LimitRepository extends JpaRepository<Limit, Integer> {
    
    List<Limit> findByUser(User user);
    
    Optional<Limit> findByUserAndCategory(User user, Category category);
    
    List<Limit> findByUserAndCategoryId(User user, Integer categoryId);
    
    Optional<Limit> findByIdAndUser(Integer id, User user);
    
    boolean existsByCategory_IdAndPeriodAndUser(Integer categoryId, LimitPeriod period, User user);
} 