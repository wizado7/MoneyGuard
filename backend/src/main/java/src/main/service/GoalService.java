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

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public List<GoalResponse> getAllGoals() {
        User currentUser = getCurrentUser();
        Integer userId = currentUser.getId();

        List<Goal> goals = goalRepository.findByUserId(userId);
        log.info("Найдено {} целей для пользователя {}", goals.size(), currentUser.getEmail());
        return goals.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public GoalResponse getGoalById(Integer id) {
        User currentUser = getCurrentUser();
        Integer userId = currentUser.getId();

        Goal goal = goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Цель", id));
        return convertToResponse(goal);
    }

    public GoalResponse createGoal(GoalRequest goalRequest) {
        log.debug("Создание новой цели: {}", goalRequest.getName());
        
        User currentUser = getCurrentUser();
        
        // Валидация данных
        validateGoalRequest(goalRequest);
        
        // Преобразование строкового приоритета в enum
        GoalPriority priority;
        try {
            priority = GoalPriority.valueOf(goalRequest.getPriority());
        } catch (IllegalArgumentException e) {
            log.warn("Некорректный приоритет: {}", goalRequest.getPriority());
            throw new InvalidDataException("Некорректный приоритет")
                    .addError("priority", "Приоритет должен быть одним из: LOW, MEDIUM, HIGH, CRITICAL");
        }
        
        // Создание новой цели
        Goal goal = Goal.builder()
                .name(goalRequest.getName())
                .description(null)
                .targetAmount(goalRequest.getTarget_amount())
                .currentAmount(BigDecimal.ZERO)
                .targetDate(goalRequest.getTarget_date())
                .priority(priority)
                .createdAt(LocalDateTime.now())
                .user(currentUser)
                .build();
        
        Goal savedGoal = goalRepository.save(goal);
        log.info("Цель успешно создана: {}", savedGoal.getId());
        
        return convertToResponse(savedGoal);
    }

    public GoalDetailResponse getGoalDetails(Integer id) {
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
                .goal(convertToResponse(goal))
                .transactions(transactions.stream()
                        .map(this::mapTransactionToResponse)
                        .collect(Collectors.toList()))
                .progress(progress)
                .build();
    }

    @Transactional
    public GoalPlanResponse updateGoal(Integer id, GoalRequest goalRequest) {
        log.info("Обновление цели с ID: {}", id);
        User currentUser = getCurrentUser();
        Integer userId = currentUser.getId();

        Goal existingGoal = goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Цель", id));

        if (!existingGoal.getUser().getId().equals(currentUser.getId())) {
            throw new OperationNotAllowedException("Вы не можете изменять чужую цель");
        }

        // Проверка, что целевая сумма не меньше текущей
        if (goalRequest.getTarget_amount().compareTo(goalRequest.getCurrent_amount()) < 0) {
            throw new InvalidDataException("Целевая сумма не может быть меньше текущей накопленной суммы.");
        }

        existingGoal.setName(goalRequest.getName());
        existingGoal.setTargetAmount(goalRequest.getTarget_amount());
        existingGoal.setCurrentAmount(goalRequest.getCurrent_amount());
        existingGoal.setTargetDate(goalRequest.getTarget_date());

        try {
            existingGoal.setPriority(GoalPriority.valueOf(goalRequest.getPriority().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("Недопустимое значение приоритета: " + goalRequest.getPriority());
        }

        Goal updatedGoal = goalRepository.save(existingGoal);
        log.info("Цель с ID {} успешно обновлена", id);

        return calculateGoalPlan(updatedGoal);
    }

    public void deleteGoal(Integer id) {
        log.debug("Удаление цели с ID: {}", id);
        User currentUser = getCurrentUser();
        Integer userId = currentUser.getId();

        Goal goal = goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Цель", id));
        
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

    private GoalResponse convertToResponse(Goal goal) {
        if (goal == null) return null;
        return GoalResponse.builder()
                .id(goal.getId())
                .name(goal.getName())
                .target_amount(goal.getTargetAmount())
                .current_amount(goal.getCurrentAmount())
                .target_date(goal.getTargetDate())
                .priority(goal.getPriority() != null ? goal.getPriority().name() : null)
                .build();
    }

    private TransactionResponse mapTransactionToResponse(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        return TransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .category(transaction.getCategory() != null ? transaction.getCategory().getName() : null)
                .categoryId(transaction.getCategory() != null ? transaction.getCategory().getId() : null)
                .date(transaction.getDate())
                .description(transaction.getDescription())
                .goalId(transaction.getGoal() != null ? transaction.getGoal().getId() : null)
                .userId(transaction.getUser() != null ? transaction.getUser().getId().toString() : null)
                .createdAt(transaction.getCreatedAt())
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
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) == 0) {
            return 0.0; // Избегаем деления на ноль
        }
        BigDecimal progress = goal.getCurrentAmount()
                .divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP) // Увеличим точность
                .multiply(BigDecimal.valueOf(100));
        return progress.doubleValue();
    }

    private void validateGoalRequest(GoalRequest request) {
        InvalidDataException exception = new InvalidDataException("Некорректные данные цели");

        if (request.getTarget_amount().compareTo(BigDecimal.ZERO) <= 0) {
            exception.addError("target_amount", "Целевая сумма должна быть положительным числом");
        }

        // Добавим проверку для current_amount, если она нужна при создании
        // if (request.getCurrent_amount() != null && request.getCurrent_amount().compareTo(BigDecimal.ZERO) < 0) {
        //     exception.addError("current_amount", "Текущая сумма не может быть отрицательной");
        // }

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

    private GoalPlanResponse createGoalPlanResponse(Goal goal) {
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), goal.getTargetDate());
        daysLeft = Math.max(0, daysLeft); // Убедимся, что дни не отрицательные

        BigDecimal remainingAmount = goal.getTargetAmount().subtract(goal.getCurrentAmount());
        remainingAmount = remainingAmount.max(BigDecimal.ZERO); // Сумма не может быть отрицательной

        BigDecimal dailyContribution = BigDecimal.ZERO;
        if (daysLeft > 0) {
            dailyContribution = remainingAmount.divide(BigDecimal.valueOf(daysLeft), 2, RoundingMode.CEILING);
        } else if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Если срок истек, а цель не достигнута, показываем всю оставшуюся сумму как "дневной" взнос
            dailyContribution = remainingAmount;
        }

        BigDecimal monthlyContribution = dailyContribution.multiply(BigDecimal.valueOf(30));

        String monthlyPayment = String.format("%.2f", monthlyContribution);
        String optimizationAdvice = String.format("Рекомендуем ежемесячно откладывать %s рублей для достижения цели к %s.",
                monthlyPayment, goal.getTargetDate().toString());

        return GoalPlanResponse.builder()
                .id(goal.getId())
                .name(goal.getName())
                .description(goal.getDescription()) // Добавим описание, если оно есть
                .target_amount(goal.getTargetAmount())
                .current_amount(goal.getCurrentAmount())
                .target_date(goal.getTargetDate())
                .priority(goal.getPriority().name())
                .days_left(daysLeft)
                .daily_contribution(dailyContribution)
                .monthly_contribution(monthlyContribution)
                .created_at(goal.getCreatedAt())
                .monthly_payment(monthlyPayment)
                .optimization_advice(optimizationAdvice)
                .goal(convertToResponse(goal))
                .build();
    }
} 