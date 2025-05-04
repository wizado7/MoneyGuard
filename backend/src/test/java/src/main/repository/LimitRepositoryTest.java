package src.main.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import src.main.model.*;
import src.test.utils.TestDataUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static src.test.utils.TestDataUtils.*;

@DataJpaTest
@ActiveProfiles("test")
public class LimitRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LimitRepository limitRepository;

    @Test
    public void whenFindByUser_thenReturnLimits() {
        // given
        User user = entityManager.persist(createTestUser("user5@example.com"));
        Category category = entityManager.persist(createTestCategory("Entertainment"));
        Limit limit = entityManager.persist(
                createTestLimit(user, category, LimitPeriod.MONTHLY, BigDecimal.valueOf(500))
        );
        entityManager.flush();

        // when
        List<Limit> limits = limitRepository.findByUser(user);

        // then
        assertAll(
                () -> assertFalse(limits.isEmpty(), "Limits list should not be empty"),
                () -> assertEquals(limit.getAmount(), limits.get(0).getAmount(), "Amounts should match"),
                () -> assertEquals(user.getId(), limits.get(0).getUser().getId(), "User IDs should match"),
                () -> assertEquals(LimitPeriod.MONTHLY, limits.get(0).getPeriod(), "Periods should match")
        );
    }

    @Test
    public void whenFindByUserAndCategory_thenReturnLimit() {
        // given
        User user = entityManager.persist(createTestUser("user6@example.com"));
        Category category = entityManager.persist(createTestCategory("Shopping"));
        Limit limit = entityManager.persist(
                createTestLimit(user, category, LimitPeriod.WEEKLY, BigDecimal.valueOf(300))
        );
        entityManager.flush();

        // when
        Optional<Limit> found = limitRepository.findByUserAndCategory(user, category);

        // then
        assertAll(
                () -> assertTrue(found.isPresent(), "Limit should be found"),
                () -> assertEquals(limit.getPeriod(), found.get().getPeriod(), "Periods should match"),
                () -> assertEquals(category.getId(), found.get().getCategory().getId(), "Category IDs should match")
        );
    }

    @Test
    public void whenFindByUserAndNonExistentCategory_thenReturnEmpty() {
        // given
        User user = entityManager.persist(createTestUser("user7@example.com"));
        Category category1 = entityManager.persist(createTestCategory("Food"));
        Category category2 = entityManager.persist(createTestCategory("Transport"));
        entityManager.persist(
                createTestLimit(user, category1, LimitPeriod.DAILY, BigDecimal.valueOf(100))
        );
        entityManager.flush();

        // when
        Optional<Limit> found = limitRepository.findByUserAndCategory(user, category2);

        // then
        assertFalse(found.isPresent(), "Limit should not be found");
    }

    @Test
    public void whenExistsByCategoryAndPeriodAndUser_thenReturnTrue() {
        // given
        User user = entityManager.persist(createTestUser("user8@example.com"));
        Category category = entityManager.persist(createTestCategory("Utilities"));
        entityManager.persist(
                createTestLimit(user, category, LimitPeriod.MONTHLY, BigDecimal.valueOf(200))
        );
        entityManager.flush();

        // when
        boolean exists = limitRepository.existsByCategory_IdAndPeriodAndUser(
                category.getId(), LimitPeriod.MONTHLY, user);

        // then
        assertTrue(exists, "Limit should exist");
    }
}