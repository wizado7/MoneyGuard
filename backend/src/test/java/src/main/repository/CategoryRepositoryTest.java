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
public class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    public void whenFindByName_thenReturnCategory() {
        // given
        Category category = entityManager.persist(createTestCategory("Food"));
        entityManager.flush();

        // when
        Optional<Category> found = categoryRepository.findByName(category.getName());

        // then
        assertAll(
                () -> assertTrue(found.isPresent(), "Category should be found"),
                () -> assertEquals(category.getName(), found.get().getName(), "Names should match"),
                () -> assertFalse(found.get().isIncome(), "Should not be income category"),
                () -> assertNull(found.get().getParent(), "Parent should be null for root category")
        );
    }

    @Test
    public void whenFindByNonExistentName_thenReturnEmpty() {
        // when
        Optional<Category> found = categoryRepository.findByName("NonExistentCategory");

        // then
        assertFalse(found.isPresent(), "Category should not be found");
    }

    @Test
    public void whenExistsByName_thenReturnTrue() {
        // given
        Category category = entityManager.persist(createTestCategory("Transport"));
        entityManager.flush();

        // when
        boolean exists = categoryRepository.existsByName(category.getName());

        // then
        assertTrue(exists, "Category should exist");
    }

    @Test
    public void whenExistsByNonExistentName_thenReturnFalse() {
        // when
        boolean exists = categoryRepository.existsByName("NonExistentCategory");

        // then
        assertFalse(exists, "Category should not exist");
    }

    @Test
    public void whenFindByParentId_thenReturnCategories() {
        // given
        Category parent = entityManager.persist(createTestCategory("Parent"));
        Category child = entityManager.persist(
                Category.builder()
                        .name("Child")
                        .parent(parent)
                        .isIncome(false)
                        .isSystem(false)
                        .build()
        );
        entityManager.flush();

        // when
        List<Category> children = categoryRepository.findByParentId(parent.getId());

        // then
        assertAll(
                () -> assertFalse(children.isEmpty(), "Children list should not be empty"),
                () -> assertEquals(child.getName(), children.get(0).getName(), "Names should match"),
                () -> assertEquals(parent.getId(), children.get(0).getParent().getId(), "Parent IDs should match")
        );
    }

    @Test
    public void whenFindByParentIdWithNoChildren_thenReturnEmptyList() {
        // given
        Category parent = entityManager.persist(createTestCategory("ParentWithNoChildren"));
        entityManager.flush();

        // when
        List<Category> children = categoryRepository.findByParentId(parent.getId());

        // then
        assertTrue(children.isEmpty(), "Children list should be empty");
    }
}