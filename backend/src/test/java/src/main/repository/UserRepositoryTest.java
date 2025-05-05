package src.main.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import src.main.model.User;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static src.test.utils.TestDataUtils.createTestUser;

@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void whenFindByEmail_thenReturnUser() {
        // given
        User user = entityManager.persist(createTestUser("test@example.com"));
        entityManager.flush();

        // when
        Optional<User> found = userRepository.findByEmail(user.getEmail());

        // then
        assertTrue(found.isPresent(), "User should be found by email");
        assertEquals(user.getEmail(), found.get().getEmail(), "Emails should match");
        assertNotNull(found.get().getCreatedAt(), "CreatedAt should not be null");
    }

    @Test
    public void whenFindByNonExistentEmail_thenReturnEmpty() {
        // when
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // then
        assertFalse(found.isPresent(), "User should not be found");
    }

    @Test
    public void whenExistsByEmail_thenReturnTrue() {
        // given
        User user = entityManager.persist(createTestUser("exists@example.com"));
        entityManager.flush();

        // when
        boolean exists = userRepository.existsByEmail(user.getEmail());

        // then
        assertTrue(exists, "User should exist");
    }

    @Test
    public void whenExistsByNonExistentEmail_thenReturnFalse() {
        // when
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // then
        assertFalse(exists, "User should not exist");
    }
}