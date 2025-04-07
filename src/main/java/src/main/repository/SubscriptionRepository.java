package src.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import src.main.model.Subscription;
import src.main.model.User;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUser(User user);
} 