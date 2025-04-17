package src.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import src.main.model.Category;
// import src.main.model.User; // Больше не нужен
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // Удаляем или комментируем методы, специфичные для пользователя
    // List<Category> findByUser(User user);
    // Optional<Category> findByIdAndUser(Integer id, User user);
    // Optional<Category> findByNameAndUser(String name, User user);
    // boolean existsByNameAndUser(String name, User user);
    // List<Category> findByUserIdOrUserIdIsNull(Integer userId);
    // Optional<Category> findByIdAndUserIdOrUserIdIsNull(Integer id, Integer userId);
    // boolean existsByNameAndUserIdIsNull(String name);

    // Оставляем/добавляем глобальные методы
    Optional<Category> findByName(String name);
    boolean existsByName(String name);
    List<Category> findByParentId(Integer parentId); // Для иерархии
} 