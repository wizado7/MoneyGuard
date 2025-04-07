package src.main.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import src.main.dto.limit.LimitRequest;
import src.main.dto.limit.LimitResponse;
import src.main.service.LimitService;
import src.main.exception.BusinessException;
import src.main.exception.EntityNotFoundException;
import src.main.exception.OperationNotAllowedException;
import src.main.exception.InvalidDataException;
import src.main.exception.ConflictException;

import java.util.List;

@RestController
@RequestMapping("/limits")
@RequiredArgsConstructor
@Validated
@Slf4j
public class LimitController {

    private final LimitService limitService;

    @GetMapping
    public ResponseEntity<List<LimitResponse>> getLimits() {
        log.debug("REST запрос на получение списка лимитов");
        try {
            return ResponseEntity.ok(limitService.getLimits());
        } catch (Exception e) {
            log.error("Ошибка при получении списка лимитов: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при получении списка лимитов", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<LimitResponse> createLimit(@RequestBody @Valid LimitRequest request) {
        log.debug("REST запрос на создание нового лимита");
        try {
            LimitResponse response = limitService.createLimit(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (EntityNotFoundException | OperationNotAllowedException | InvalidDataException | ConflictException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при создании лимита: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при создании лимита", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<LimitResponse> updateLimit(
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id,
            @RequestBody @Valid LimitRequest request) {
        log.debug("REST запрос на обновление лимита с ID: {}", id);
        try {
            return ResponseEntity.ok(limitService.updateLimit(id, request));
        } catch (EntityNotFoundException | OperationNotAllowedException | InvalidDataException | ConflictException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при обновлении лимита с ID {}: {}", id, e.getMessage(), e);
            throw new BusinessException("Ошибка при обновлении лимита", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLimit(
            @PathVariable @Positive(message = "ID должен быть положительным числом") Long id) {
        log.debug("REST запрос на удаление лимита с ID: {}", id);
        try {
            limitService.deleteLimit(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException | OperationNotAllowedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при удалении лимита с ID {}: {}", id, e.getMessage(), e);
            throw new BusinessException("Ошибка при удалении лимита", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 