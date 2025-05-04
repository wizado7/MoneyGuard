package src.main.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import src.main.model.*;
import src.test.utils.TestDataUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static src.test.utils.TestDataUtils.*;

@DataJpaTest
@ActiveProfiles("test")
public class SubscriptionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    public void whenFindByUser_thenReturnSubscription() {
        // given
        User user = entityManager.persist(createTestUser("user7@example.com"));
        Subscription subscription = entityManager.persist(
                createTestSubscription(user, SubscriptionType.PREMIUM, LocalDate.now().plusMonths(1))
        );
        entityManager.flush();

        // when
        Optional<Subscription> found = subscriptionRepository.findByUser(user);

        // then
        assertAll(
                () -> assertTrue(found.isPresent(), "Subscription should be found"),
                () -> assertEquals(subscription.getType(), found.get().getType(), "Subscription types should match"),
                () -> assertEquals(user.getId(), found.get().getUser().getId(), "User IDs should match"),
                () -> assertNotNull(found.get().getExpiresAt(), "Expiration date should not be null")
        );
    }

    @Test
    public void whenFindByNonExistentUser_thenReturnEmpty() {
        // given
        User user1 = entityManager.persist(createTestUser("user1@example.com"));
        User user2 = entityManager.persist(createTestUser("user2@example.com"));
        entityManager.persist(
                createTestSubscription(user1, SubscriptionType.PREMIUM, LocalDate.now().plusMonths(1))
        );
        entityManager.flush();

        // when
        Optional<Subscription> found = subscriptionRepository.findByUser(user2);

        // then
        assertFalse(found.isPresent(), "Subscription should not be found");
    }

    @Test
    public void whenFindExpiredSubscription_thenReturnSubscription() {
        // given
        User user = entityManager.persist(createTestUser("user8@example.com"));
        Subscription subscription = entityManager.persist(
                createTestSubscription(user, SubscriptionType.FREE, LocalDate.now().minusDays(1))
        );
        entityManager.flush();

        // when
        Optional<Subscription> found = subscriptionRepository.findByUser(user);

        // then
        assertAll(
                () -> assertTrue(found.isPresent(), "Expired subscription should still be found"),
                () -> assertTrue(found.get().getExpiresAt().isBefore(LocalDate.now()),
                        "Subscription should be expired")
        );
    }
}