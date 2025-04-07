package src.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import src.main.model.Category;
import src.main.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    List<Category> findByUser(User user);
    
    List<Category> findByParentId(Long parentId);
    
    Optional<Category> findByNameAndUser(String name, User user);
} 