package src.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import src.main.model.Goal;
import src.main.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Integer> {
    List<Goal> findByUserId(Integer userId);
    List<Goal> findByUserIdOrderByTargetDateAsc(Integer userId);
    Optional<Goal> findByIdAndUserId(Integer goalId, Integer userId);
} 