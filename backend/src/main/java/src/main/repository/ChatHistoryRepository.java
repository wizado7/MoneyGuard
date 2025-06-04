package src.main.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import src.main.model.ChatHistory;
import src.main.model.User;

import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Integer> {
    
    List<ChatHistory> findByUserOrderByCreatedAtAsc(User user);
    
    Page<ChatHistory> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    @Modifying
    @Transactional
    void deleteAllByUser(User user);
    
    // Получить последние N сообщений для контекста
    @Query("SELECT ch FROM ChatHistory ch WHERE ch.user = :user ORDER BY ch.createdAt DESC")
    List<ChatHistory> findLastNMessages(@Param("user") User user, Pageable pageable);
}

