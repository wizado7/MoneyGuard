package src.main.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import src.main.dto.goal.GoalDetailResponse;
import src.main.dto.goal.GoalPlanResponse;
import src.main.dto.goal.GoalRequest;
import src.main.dto.goal.GoalResponse;
import src.main.service.GoalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import src.main.exception.EntityNotFoundException;
import src.main.exception.OperationNotAllowedException;
import src.main.exception.InvalidDataException;
import src.main.exception.BusinessException;
import src.main.exception.ConflictException;

import java.util.List;

@RestController
@RequestMapping("/goals")
@RequiredArgsConstructor
@Validated
@Slf4j
public class GoalController {

    private final GoalService goalService;

    @GetMapping
    public ResponseEntity<List<GoalResponse>> getAllGoals() {
        log.debug("REST запрос на получение списка целей");
        return ResponseEntity.ok(goalService.getAllGoals());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalResponse> getGoalById(@PathVariable Integer id) {
        log.debug("REST запрос на получение цели с ID: {}", id);
        return ResponseEntity.ok(goalService.getGoalById(id));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<GoalDetailResponse> getGoalDetails(@PathVariable Integer id) {
        log.debug("REST запрос на получение детальной информации о цели с ID: {}", id);
        try {
            return ResponseEntity.ok(goalService.getGoalDetails(id));
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (OperationNotAllowedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при получении цели с ID {}: {}", id, e.getMessage(), e);
            throw new BusinessException("Ошибка при получении цели", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(@RequestBody @Valid GoalRequest goalRequest) {
        log.debug("REST запрос на создание цели: {}", goalRequest.getName());
        try {
            GoalResponse response = goalService.createGoal(goalRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (EntityNotFoundException | OperationNotAllowedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при создании цели: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при создании цели: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalPlanResponse> updateGoal(@PathVariable Integer id, @RequestBody @Valid GoalRequest goalRequest) {
        log.debug("REST запрос на обновление цели с ID: {}", id);
        try {
            return ResponseEntity.ok(goalService.updateGoal(id, goalRequest));
        } catch (EntityNotFoundException | InvalidDataException | OperationNotAllowedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при обновлении цели с ID {}: {}", id, e.getMessage(), e);
            throw new BusinessException("Ошибка при обновлении цели", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Integer id) {
        log.debug("REST запрос на удаление цели с ID: {}", id);
        try {
            goalService.deleteGoal(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException | OperationNotAllowedException | ConflictException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при удалении цели с ID {}: {}", id, e.getMessage(), e);
            throw new BusinessException("Ошибка при удалении цели", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 