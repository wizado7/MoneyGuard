package src.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import src.main.dto.goal.GoalDetailResponse;
import src.main.dto.goal.GoalPlanResponse;
import src.main.dto.goal.GoalRequest;
import src.main.dto.goal.GoalResponse;
import src.main.dto.transaction.TransactionResponse;
import src.main.exception.AccessDeniedException;
import src.main.exception.EntityNotFoundException;
import src.main.exception.InvalidDataException;
import src.main.exception.OperationNotAllowedException;
import src.main.exception.ResourceNotFoundException;
import src.main.model.Goal;
import src.main.model.GoalPriority;
import src.main.model.Transaction;
import src.main.model.User;
import src.main.repository.GoalRepository;
import src.main.repository.TransactionRepository;
import src.main.repository.UserRepository;
import src.main.exception.ConflictException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public List<GoalResponse> getGoals() {
        log.debug("Получение списка целей");
        User currentUser = getCurrentUser();
        
        List<Goal> goals = goalRepository.findByUser(currentUser);
        log.info("Получено {} целей для пользователя: {}", goals.size(), currentUser.getEmail());
        
        return goals.stream()
                .map(this::mapToGoalResponse)
                .collect(Collectors.toList());
    }

    public GoalResponse createGoal(GoalRequest request) {
        log.debug("Создание новой цели: {}", request.getName());
        
        User currentUser = getCurrentUser();
        
        // Валидация данных
        validateGoalRequest(request);
        
        // Преобразование строкового приоритета в enum
        GoalPriority priority;
        try {
            priority = GoalPriority.valueOf(request.getPriority());
        } catch (IllegalArgumentException e) {
            log.warn("Некорректный приоритет: {}", request.getPriority());
            throw new InvalidDataException("Некорректный приоритет")
                    .addError("priority", "Приоритет должен быть одним из: LOW, MEDIUM, HIGH, CRITICAL");
        }
        
        // Создание новой цели
        Goal goal = Goal.builder()
                .name(request.getName())
                .description(null)
                .targetAmount(request.getTarget_amount())
                .currentAmount(BigDecimal.ZERO)
                .targetDate(request.getTarget_date())
                .priority(priority)
                .createdAt(LocalDateTime.now())
                .user(currentUser)
                .build();
        
        Goal savedGoal = goalRepository.save(goal);
        log.info("Цель успешно создана: {}", savedGoal.getId());
        
        return mapToGoalResponse(savedGoal);
    }

    public GoalDetailResponse getGoalDetails(Long id) {
        log.debug("Получение детальной информации о цели с ID: {}", id);
        User currentUser = getCurrentUser();
        
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Цель с ID {} не найдена", id);
                    return new EntityNotFoundException("Цель", id);
                });
        
        // Проверяем, что цель принадлежит текущему пользователю
        if (!goal.getUser().getId().equals(currentUser.getId())) {
            log.warn("Попытка доступа к чужой цели: {} пользователем: {}", id, currentUser.getEmail());
            throw OperationNotAllowedException.notOwner("цель");
        }
        
        log.info("Получена детальная информация о цели: {} для пользователя: {}", goal.getName(), currentUser.getEmail());
        
        // Получаем список транзакций, связанных с целью
        List<Transaction> transactions = new ArrayList<>(); // Заглушка, пока нет связи с транзакциями
        
        // Рассчитываем прогресс
        double progress = calculateProgress(goal);
        
        return GoalDetailResponse.builder()
                .goal(mapToGoalResponse(goal))
                .transactions(transactions.stream()
                        .map(this::mapToTransactionResponse)
                        .collect(Collectors.toList()))
                .progress(progress)
                .build();
    }

    public GoalPlanResponse updateGoal(Long id, GoalRequest request) {
        log.debug("Обновление цели с ID: {}", id);
        User currentUser = getCurrentUser();
        
        // Проверка входных данных
        if (request.getTarget_date() != null && request.getTarget_date().isBefore(LocalDate.now())) {
            log.warn("Попытка установить целевую дату в прошлом: {}", request.getTarget_date());
            throw new InvalidDataException("Целевая дата не может быть в прошлом")
                    .addError("target_date", "Целевая дата не может быть в прошлом");
        }
        
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Цель с ID {} не найдена", id);
                    return new EntityNotFoundException("Цель", id);
                });
        
        // Проверяем, что цель принадлежит текущему пользователю
        if (!goal.getUser().getId().equals(currentUser.getId())) {
            log.warn("Попытка обновления чужой цели: {} пользователем: {}", id, currentUser.getEmail());
            throw OperationNotAllowedException.notOwner("цель");
        }
        
        if (request.getName() != null && !request.getName().isEmpty()) {
            goal.setName(request.getName());
        }
        
        if (request.getTarget_amount() != null) {
            goal.setTargetAmount(request.getTarget_amount());
        }
        
        if (request.getTarget_date() != null) {
            goal.setTargetDate(request.getTarget_date());
        }
        
        if (request.getPriority() != null) {
            try {
                goal.setPriority(GoalPriority.valueOf(request.getPriority()));
            } catch (IllegalArgumentException e) {
                log.warn("Некорректное значение приоритета: {}", request.getPriority());
                throw new InvalidDataException("Некорректное значение приоритета")
                        .addError("priority", "Приоритет должен быть одним из: LOW, MEDIUM, HIGH");
            }
        }
        
        goalRepository.save(goal);
        log.info("Цель успешно обновлена: {} для пользователя: {}", goal.getName(), currentUser.getEmail());
        
        // Рассчитываем ежемесячный платеж
        BigDecimal monthlyPayment = calculateMonthlyPayment(goal);
        
        return GoalPlanResponse.builder()
                .goal(mapToGoalResponse(goal))
                .monthly_payment(monthlyPayment)
                .optimization_advice(generateOptimizationAdvice(goal, monthlyPayment))
                .build();
    }

    public void deleteGoal(Long id) {
        log.debug("Удаление цели с ID: {}", id);
        User currentUser = getCurrentUser();
        
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Цель с ID {} не найдена", id);
                    return new EntityNotFoundException("Цель", id);
                });
        
        // Проверяем, что цель принадлежит текущему пользователю
        if (!goal.getUser().getId().equals(currentUser.getId())) {
            log.warn("Попытка удаления чужой цели: {} пользователем: {}", id, currentUser.getEmail());
            throw OperationNotAllowedException.notOwner("цель");
        }
        
        // Проверяем, есть ли связанные транзакции
        List<Transaction> transactions = transactionRepository.findByUserAndGoal(currentUser, goal);
        if (!transactions.isEmpty()) {
            log.warn("Попытка удаления цели с связанными транзакциями: {}", id);
            throw ConflictException.dependencyExists("цель", "транзакции");
        }
        
        goalRepository.delete(goal);
        log.info("Цель успешно удалена: {} для пользователя: {}", goal.getName(), currentUser.getEmail());
    }

    private GoalResponse mapToGoalResponse(Goal goal) {
        return GoalResponse.builder()
                .id(goal.getId())
                .name(goal.getName())
                .target_amount(goal.getTargetAmount())
                .current_amount(goal.getCurrentAmount())
                .target_date(goal.getTargetDate())
                .priority(goal.getPriority().name())
                .build();
    }

    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .category(transaction.getCategory().getName())
                .date(transaction.getDate())
                .description(transaction.getDescription())
                .created_at(transaction.getCreatedAt())
                .build();
    }

    private BigDecimal calculateMonthlyPayment(Goal goal) {
        // Рассчитываем количество месяцев до целевой даты
        long months = ChronoUnit.MONTHS.between(LocalDate.now(), goal.getTargetDate());
        if (months <= 0) {
            months = 1; // Минимум 1 месяц
        }
        
        // Рассчитываем ежемесячный платеж
        BigDecimal remainingAmount = goal.getTargetAmount().subtract(goal.getCurrentAmount());
        return remainingAmount.divide(BigDecimal.valueOf(months), 2, RoundingMode.CEILING);
    }

    private String generateOptimizationAdvice(Goal goal, BigDecimal monthlyPayment) {
        return "Рекомендуем ежемесячно откладывать " + monthlyPayment + " рублей для достижения цели к " + goal.getTargetDate() + ".";
    }

    private double calculateProgress(Goal goal) {
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        
        double progress = goal.getCurrentAmount().divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP).doubleValue() * 100;
        return Math.min(progress, 100); // Ограничиваем прогресс 100%
    }

    private void validateGoalRequest(GoalRequest request) {
        InvalidDataException exception = new InvalidDataException("Некорректные данные цели");
        
        if (request.getTarget_amount().compareTo(BigDecimal.ZERO) <= 0) {
            exception.addError("target_amount", "Целевая сумма должна быть положительным числом");
        }
        
        if (request.getTarget_date().isBefore(LocalDate.now())) {
            exception.addError("target_date", "Целевая дата должна быть в будущем");
        }
        
        if (!exception.getErrors().isEmpty()) {
            throw exception;
        }
    }
    
    private GoalPlanResponse calculateGoalPlan(Goal goal) {
        // Расчет количества дней до целевой даты
        long daysUntilTarget = ChronoUnit.DAYS.between(LocalDate.now(), goal.getTargetDate());
        
        // Расчет ежедневного взноса
        BigDecimal dailyContribution = BigDecimal.ZERO;
        if (daysUntilTarget > 0) {
            dailyContribution = goal.getTargetAmount().divide(BigDecimal.valueOf(daysUntilTarget), 2, java.math.RoundingMode.CEILING);
        }
        
        // Расчет ежемесячного взноса (приблизительно)
        BigDecimal monthlyContribution = dailyContribution.multiply(BigDecimal.valueOf(30));
        
        return GoalPlanResponse.builder()
                .id(goal.getId())
                .name(goal.getName())
                .description(goal.getDescription())
                .target_amount(goal.getTargetAmount())
                .current_amount(goal.getCurrentAmount())
                .target_date(goal.getTargetDate())
                .priority(goal.getPriority().name())
                .days_left(daysUntilTarget)
                .daily_contribution(dailyContribution)
                .monthly_contribution(monthlyContribution)
                .created_at(goal.getCreatedAt())
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Пользователь не найден: {}", email);
                    return new ResourceNotFoundException("Пользователь не найден");
                });
    }
} 