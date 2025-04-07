package src.main.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import src.main.dto.category.CategoryRequest;
import src.main.dto.category.CategoryResponse;
import src.main.model.Category;
import src.main.model.User;
import src.main.repository.CategoryRepository;
import src.main.repository.UserRepository;
import src.main.exception.EntityNotFoundException;
import src.main.exception.OperationNotAllowedException;
import src.main.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import src.main.model.Transaction;
import src.main.repository.TransactionRepository;
import src.main.model.Limit;
import src.main.repository.LimitRepository;
import src.main.exception.InvalidDataException;
import src.main.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final LimitRepository limitRepository;
    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    public List<CategoryResponse> getCategories() {
        User currentUser = getCurrentUser();
        
        List<Category> categories = categoryRepository.findByUser(currentUser);
        
        return categories.stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        User currentUser = getCurrentUser();
        
        // Проверка родительской категории, если указана и не равна 0
        Category parentCategory = null;
        if (request.getParent_id() != null && request.getParent_id() != 0) {
            parentCategory = categoryRepository.findById(request.getParent_id())
                    .orElseThrow(() -> new EntityNotFoundException("Родительская категория", request.getParent_id()));
            
            // Проверяем, что родительская категория принадлежит текущему пользователю
            if (!parentCategory.getUser().getId().equals(currentUser.getId())) {
                throw new OperationNotAllowedException("Родительская категория не принадлежит текущему пользователю");
            }
        }
        
        // Проверка на длину иконки
        if (request.getIcon() != null && request.getIcon().length() > 50) {
            throw new InvalidDataException("Некорректная иконка")
                    .addError("icon", "Длина иконки не должна превышать 50 символов");
        }
        
        Category category = Category.builder()
                .name(request.getName())
                .icon(request.getIcon())
                .parent(parentCategory)
                .user(currentUser)
                .build();
        
        try {
            categoryRepository.save(category);
            return mapToCategoryResponse(category);
        } catch (Exception e) {
            log.error("Ошибка при сохранении категории: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при создании категории", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        User currentUser = getCurrentUser();
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Категория не найдена"));
        
        // Проверяем, что категория принадлежит текущему пользователю
        if (!category.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Доступ запрещен");
        }
        
        Category parentCategory = null;
        if (request.getParent_id() != null) {
            parentCategory = categoryRepository.findById(request.getParent_id())
                    .orElseThrow(() -> new RuntimeException("Родительская категория не найдена"));
            
            // Проверяем, что родительская категория принадлежит текущему пользователю
            if (!parentCategory.getUser().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Доступ запрещен");
            }
        }
        
        category.setName(request.getName());
        category.setParent(parentCategory);
        category.setIcon(request.getIcon());
        
        Category updatedCategory = categoryRepository.save(category);
        
        return mapToCategoryResponse(updatedCategory);
    }

    public void deleteCategory(Long id) {
        log.debug("Удаление категории с ID: {}", id);
        User currentUser = getCurrentUser();
        
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Категория с ID {} не найдена", id);
                    return new EntityNotFoundException("Категория", id);
                });
        
        // Проверяем, что категория принадлежит текущему пользователю
        if (!category.getUser().getId().equals(currentUser.getId())) {
            log.warn("Попытка удаления чужой категории: {} пользователем: {}", id, currentUser.getEmail());
            throw OperationNotAllowedException.notOwner("категория");
        }
        
        // Проверяем, есть ли дочерние категории
        List<Category> childCategories = categoryRepository.findByParentId(id);
        if (!childCategories.isEmpty()) {
            log.warn("Попытка удаления категории с дочерними категориями: {}", id);
            throw ConflictException.dependencyExists("категория", "дочерние категории");
        }
        
        // Проверяем, есть ли связанные транзакции
        List<Transaction> transactions = transactionRepository.findByUserAndCategoryIdOrderByDateDesc(currentUser, id);
        if (!transactions.isEmpty()) {
            log.warn("Попытка удаления категории с связанными транзакциями: {}", id);
            throw ConflictException.dependencyExists("категория", "транзакции");
        }
        
        // Проверяем, есть ли связанные лимиты
        List<Limit> limits = limitRepository.findByUserAndCategoryId(currentUser, id);
        if (!limits.isEmpty()) {
            log.warn("Попытка удаления категории с связанными лимитами: {}", id);
            throw ConflictException.dependencyExists("категория", "лимиты");
        }
        
        categoryRepository.delete(category);
        log.info("Категория успешно удалена: {} для пользователя: {}", category.getName(), currentUser.getEmail());
    }

    private CategoryResponse mapToCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .icon(category.getIcon())
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }
} 