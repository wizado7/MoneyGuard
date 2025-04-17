package src.main.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import src.main.dto.category.CategoryRequest;
import src.main.dto.category.CategoryResponse;
import src.main.model.Category;
import src.main.service.CategoryService;
import src.main.exception.BusinessException;
import src.main.exception.EntityNotFoundException;
import src.main.exception.OperationNotAllowedException;
import src.main.exception.InvalidDataException;
import src.main.exception.ConflictException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Validated
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        log.debug("REST request to get all Categories");
        List<CategoryResponse> categoryResponses = categoryService.getCategories();
        return ResponseEntity.ok(categoryResponses);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody @Valid CategoryRequest request) {
        log.debug("REST запрос на создание категории: {}", request.getName());
        try {
            CategoryResponse response = categoryService.createCategory(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (EntityNotFoundException | OperationNotAllowedException | InvalidDataException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при создании категории: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при создании категории: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable @Positive(message = "ID должен быть положительным числом") Integer id,
            @RequestBody @Valid CategoryRequest request) {
        log.debug("REST запрос на обновление категории с ID: {}", id);
        try {
            return ResponseEntity.ok(categoryService.updateCategory(id, request));
        } catch (EntityNotFoundException | OperationNotAllowedException | InvalidDataException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при обновлении категории с ID {}: {}", id, e.getMessage(), e);
            throw new BusinessException("Ошибка при обновлении категории", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable @Positive(message = "ID должен быть положительным числом") Integer id) {
        log.debug("REST запрос на удаление категории с ID: {}", id);
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException | OperationNotAllowedException | ConflictException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при удалении категории с ID {}: {}", id, e.getMessage(), e);
            throw new BusinessException("Ошибка при удалении категории", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 