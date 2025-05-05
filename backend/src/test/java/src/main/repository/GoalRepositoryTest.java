package src.main.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import src.main.model.*;
import src.test.utils.TestDataUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static src.test.utils.TestDataUtils.*;

@DataJpaTest
@ActiveProfiles("test")
public class GoalRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GoalRepository goalRepository;

    @Test
    public void whenFindByUserId_thenReturnGoals() {
        // given
        User user = entityManager.persist(createTestUser("user3@example.com"));
        Goal goal = entityManager.persist(createTestGoal(user, "New Car"));
        entityManager.flush();

        // when
        List<Goal> goals = goalRepository.findByUserId(user.getId());

        // then
        assertFalse(goals.isEmpty(), "Goals list should not be empty");
        assertEquals(goal.getName(), goals.get(0).getName(), "Goal names should match");
        assertEquals(user.getId(), goals.get(0).getUser().getId(), "User IDs should match");
    }

    @Test
    public void whenFindByIdAndUserId_thenReturnGoal() {
        // given
        User user = entityManager.persist(createTestUser("user4@example.com"));
        Goal goal = entityManager.persist(createTestGoal(user, "Vacation"));
        entityManager.flush();

        // when
        Optional<Goal> found = goalRepository.findByIdAndUserId(goal.getId(), user.getId());

        // then
        assertTrue(found.isPresent(), "Goal should be found");
        assertEquals(goal.getName(), found.get().getName(), "Goal names should match");
        assertEquals(goal.getTargetAmount(), found.get().getTargetAmount(), "Target amounts should match");
    }

    @Test
    public void whenFindByIdWithWrongUserId_thenReturnEmpty() {
        // given
        User user1 = entityManager.persist(createTestUser("user1@example.com"));
        User user2 = entityManager.persist(createTestUser("user2@example.com"));
        Goal goal = entityManager.persist(createTestGoal(user1, "Shared Goal"));
        entityManager.flush();

        // when
        Optional<Goal> found = goalRepository.findByIdAndUserId(goal.getId(), user2.getId());

        // then
        assertFalse(found.isPresent(), "Goal should not be found for wrong user");
    }
}