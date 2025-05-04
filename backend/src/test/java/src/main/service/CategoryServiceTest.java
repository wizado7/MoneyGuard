package src.main.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import src.main.dto.category.CategoryRequest;
import src.main.dto.category.CategoryResponse;
import src.main.model.Category;
import src.main.repository.CategoryRepository;
import src.main.exception.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1);
        testCategory.setName("Food");
        testCategory.setIncome(false);
    }

    @Test
    void getCategories_ShouldReturnAllCategories() {
        // Arrange
        when(categoryRepository.findAll()).thenReturn(List.of(testCategory));

        // Act
        List<CategoryResponse> result = categoryService.getCategories();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Food", result.get(0).getName());
    }

    @Test
    void createCategory_ShouldCreateNewCategory() {
        // Arrange
        CategoryRequest request = new CategoryRequest();
        request.setName("Transport");
        request.setIsIncome(false);

        when(categoryRepository.existsByName("Transport")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(2);
            return c;
        });

        // Act
        CategoryResponse result = categoryService.createCategory(request);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getId());
        assertEquals("Transport", result.getName());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void createCategory_ShouldThrowWhenNameExists() {
        // Arrange
        CategoryRequest request = new CategoryRequest();
        request.setName("Food");

        when(categoryRepository.existsByName("Food")).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> categoryService.createCategory(request));
    }

    @Test
    void updateCategory_ShouldUpdateCategory() {
        // Arrange
        CategoryRequest request = new CategoryRequest();
        request.setName("Updated Food");
        request.setIsIncome(false);

        when(categoryRepository.findById(1)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByName("Updated Food")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        // Act
        CategoryResponse result = categoryService.updateCategory(1, request);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Food", result.getName());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void deleteCategory_ShouldDeleteCategory() {
        // Arrange
        when(categoryRepository.findById(1)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.findByParentId(1)).thenReturn(List.of());

        // Act
        categoryService.deleteCategory(1);

        // Assert
        verify(categoryRepository, times(1)).delete(testCategory);
    }
}