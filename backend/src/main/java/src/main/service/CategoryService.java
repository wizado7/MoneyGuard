package src.main.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import src.main.dto.category.CategoryRequest;
import src.main.dto.category.CategoryResponse;
import src.main.exception.BusinessException;
import src.main.exception.DuplicateResourceException;
import src.main.model.Category;
import src.main.repository.CategoryRepository;
import src.main.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getCategories() {
        log.debug("Получение всех категорий");
        List<Category> categories = categoryRepository.findAll();
        log.info("Найдено {} категорий", categories.size());
        return categories.stream()
                .map(this::mapCategoryToResponse)
                .collect(Collectors.toList());
    }

    public CategoryResponse getCategoryById(Integer id) {
        log.debug("Получение категории по ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Категория с ID " + id + " не найдена"));
        return mapCategoryToResponse(category);
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        log.debug("Создание новой категории: {}", request.getName());

        if (categoryRepository.existsByName(request.getName())) {
            log.warn("Попытка создать категорию с существующим именем: {}", request.getName());
            throw new DuplicateResourceException("Категория с названием '" + request.getName() + "' уже существует.");
        }

        Category category = Category.builder()
                .name(request.getName())
                .icon(request.getIcon())
                .iconName(request.getIcon())
                .color(request.getColor())
                .isIncome(request.getIsIncome() != null && request.getIsIncome())
                .isSystem(false)
                .build();

        if (request.getParent_id() != null) {
            Category parent = categoryRepository.findById(request.getParent_id())
                    .orElseThrow(() -> new EntityNotFoundException("Родительская категория с ID " + request.getParent_id() + " не найдена"));
            category.setParent(parent);
        }

        Category savedCategory = categoryRepository.save(category);
        log.info("Категория '{}' (ID: {}) успешно создана", savedCategory.getName(), savedCategory.getId());
        return mapCategoryToResponse(savedCategory);
    }

    public CategoryResponse updateCategory(Integer id, CategoryRequest request) {
        log.debug("Обновление категории с ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Категория с ID " + id + " не найдена"));

        if (category.isSystem()) {
             log.warn("Попытка изменить системную категорию с ID: {}", id);
             throw new BusinessException("Системные категории не могут быть изменены", HttpStatus.BAD_REQUEST);
        }

        if (!category.getName().equalsIgnoreCase(request.getName()) && categoryRepository.existsByName(request.getName())) {
            log.warn("Попытка обновить категорию ID: {}, новое имя '{}' уже существует", id, request.getName());
            throw new DuplicateResourceException("Категория с названием '" + request.getName() + "' уже существует.");
        }

        category.setName(request.getName());
        category.setIcon(request.getIcon());
        category.setIconName(request.getIcon());
        category.setColor(request.getColor());
        category.setIncome(request.getIsIncome() != null && request.getIsIncome());

        Integer requestedParentId = request.getParent_id();
        Integer currentParentId = category.getParent() != null ? category.getParent().getId() : null;

        if (requestedParentId != null) {
            if (!requestedParentId.equals(currentParentId)) {
                 Category parent = categoryRepository.findById(requestedParentId)
                    .orElseThrow(() -> new EntityNotFoundException("Родительская категория с ID " + requestedParentId + " не найдена"));
                 if (parent.getId().equals(category.getId())) {
                     throw new BusinessException("Категория не может быть родителем самой себе", HttpStatus.BAD_REQUEST);
                 }
                 category.setParent(parent);
            }
        } else if (currentParentId != null) {
            category.setParent(null);
        }

        Category updatedCategory = categoryRepository.save(category);
        log.info("Категория с ID {} успешно обновлена", id);
        return mapCategoryToResponse(updatedCategory);
    }

    public void deleteCategory(Integer id) {
        log.debug("Удаление категории с ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Категория с ID " + id + " не найдена"));

        if (category.isSystem()) {
             log.warn("Попытка удалить системную категорию с ID: {}", id);
             throw new BusinessException("Системные категории не могут быть удалены", HttpStatus.BAD_REQUEST);
         }

        if (!categoryRepository.findByParentId(id).isEmpty()) {
            log.warn("Попытка удалить категорию ID: {} с дочерними категориями", id);
            throw new BusinessException("Нельзя удалить категорию, у которой есть дочерние категории", HttpStatus.CONFLICT);
        }

        categoryRepository.delete(category);
        log.info("Категория с ID {} успешно удалена", id);
    }

    private CategoryResponse mapCategoryToResponse(Category category) {
        if (category == null) {
            return null;
        }
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .icon(category.getIcon())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .isIncome(category.isIncome())
                .color(category.getColor())
                .build();
    }
}