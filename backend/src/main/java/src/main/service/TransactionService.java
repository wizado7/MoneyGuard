package src.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import src.main.dto.transaction.TransactionAIResponse;
import src.main.dto.transaction.TransactionRequest;
import src.main.dto.transaction.TransactionResponse;
import src.main.exception.EntityNotFoundException;
import src.main.exception.InvalidDataException;
import src.main.exception.OperationNotAllowedException;
import src.main.exception.ResourceNotFoundException;
import src.main.model.*;
import src.main.repository.*;
import src.main.service.ProfileService;
import src.main.util.DateUtil;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import src.main.exception.UserNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final LimitRepository limitRepository;
    private final ProfileService profileService;

    public List<TransactionResponse> getTransactions(LocalDate dateFrom, LocalDate dateTo, String categoryName) {
        User currentUser = profileService.getCurrentUser();
        log.debug("Получение транзакций для пользователя {} за период: {} - {}, категория: {}",
                 currentUser.getEmail(), dateFrom, dateTo, categoryName);

        List<Transaction> transactions;
        LocalDateTime startDateTime = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime endDateTime = dateTo != null ? dateTo.atTime(23, 59, 59) : null;

        if (categoryName != null && !categoryName.isEmpty()) {
            Category category = categoryRepository.findByName(categoryName)
                    .orElse(null);

            if (category != null) {
                if (startDateTime != null && endDateTime != null) {
                    transactions = transactionRepository.findByUserAndCategoryIdAndDateBetweenOrderByDateDesc(currentUser, category.getId(), startDateTime.toLocalDate(), endDateTime.toLocalDate());
                } else {
                    transactions = transactionRepository.findByUserAndCategoryIdOrderByDateDesc(currentUser, category.getId());
                }
            } else {
                 log.warn("Категория '{}' не найдена, возвращаем пустой список транзакций для этой категории", categoryName);
                 transactions = new ArrayList<>();
            }
        } else if (startDateTime != null && endDateTime != null) {
             transactions = transactionRepository.findByUserAndDateBetweenOrderByDateDesc(currentUser, startDateTime.toLocalDate(), endDateTime.toLocalDate());
        }
         else {
            transactions = transactionRepository.findByUserOrderByDateDesc(currentUser);
        }

        log.info("Найдено {} транзакций для пользователя {}", transactions.size(), currentUser.getEmail());
        return transactions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionAIResponse createTransaction(TransactionRequest request) {
        User currentUser = getCurrentUser();
        Integer userId = currentUser.getId();
        log.debug("Создание транзакции для пользователя {}", currentUser.getEmail());
        log.debug("Данные запроса: {}", request);
        
        // Логируем все поля запроса для отладки
        log.debug("amount: {}", request.getAmount());
        log.debug("categoryId: {}", request.getCategoryId());
        log.debug("date: {}", request.getDate());
        log.debug("description: {}", request.getDescription());
        log.debug("goalId: {}", request.getGoalId());
        log.debug("amountToGoal: {}", request.getAmountToGoal());

        // Получаем категорию
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Категория", request.getCategoryId()));

        // Определяем знак суммы в зависимости от типа категории (доход/расход)
        BigDecimal amount = request.getAmount().abs(); // Сначала берем абсолютное значение
        if (!category.isIncome()) {
            // Если категория расхода, делаем сумму отрицательной
            amount = amount.negate();
        }

        // Создаем новую транзакцию
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setCategory(category);
        transaction.setDate(request.getDate());
        transaction.setDescription(request.getDescription());
        transaction.setUser(currentUser);

        // Обработка цели
        if (category.isIncome() && request.getGoalId() != null) {
            Goal goal = goalRepository.findByIdAndUserId(request.getGoalId(), userId)
                    .orElseThrow(() -> new EntityNotFoundException("Цель", request.getGoalId()));

            transaction.setGoal(goal);

            // Проверяем, есть ли amountToGoal в запросе
            BigDecimal amountToGoal = null;
            
            if (request.getAmountToGoal() != null) {
                amountToGoal = request.getAmountToGoal();
                log.debug("Получена сумма для цели из запроса: {}", amountToGoal);
                
                // Проверяем, что сумма для цели не превышает сумму транзакции
                if (amountToGoal.compareTo(amount) > 0) {
                    throw new InvalidDataException("Сумма для цели не может превышать сумму транзакции");
                }
                
                // Явно устанавливаем сумму для цели в транзакции
                transaction.setAmountContributedToGoal(amountToGoal);
                
                // Добавляем сумму к цели
                goal.setCurrentAmount(goal.getCurrentAmount().add(amountToGoal));
                goalRepository.save(goal);
                
                log.info("Добавлен взнос {} к цели '{}' (ID: {})", 
                        amountToGoal, goal.getName(), goal.getId());
            } else {
                log.debug("Сумма для цели не указана в запросе, используем 0");
                // Устанавливаем 0 как сумму для цели
                transaction.setAmountContributedToGoal(BigDecimal.ZERO);
            }
        }

        // Явно логируем значение amountContributedToGoal перед сохранением
        log.debug("Значение amountContributedToGoal перед сохранением: {}", transaction.getAmountContributedToGoal());

        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Проверяем, сохранилось ли значение
        log.debug("Значение amountContributedToGoal после сохранения: {}", savedTransaction.getAmountContributedToGoal());
        
        log.info("Транзакция успешно создана с ID: {}", savedTransaction.getId());
        
        TransactionResponse transactionResponse = convertToResponse(savedTransaction);
        List<String> recommendations = generateRecommendations(savedTransaction);
        
        return new TransactionAIResponse(transactionResponse, recommendations);
    }

    @Transactional
    public TransactionResponse updateTransaction(Integer id, TransactionRequest request) {
        User currentUser = getCurrentUser();
        Integer userId = currentUser.getId();
        log.debug("Обновление транзакции с ID: {} для пользователя {}", id, currentUser.getEmail());
        log.debug("Данные запроса: {}", request);

        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Транзакция", id));

        // Получаем категорию
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Категория", request.getCategoryId()));

        // Определяем знак суммы в зависимости от типа категории (доход/расход)
        BigDecimal amount = request.getAmount().abs(); // Сначала берем абсолютное значение
        if (!category.isIncome()) {
            // Если категория расхода, делаем сумму отрицательной
            amount = amount.negate();
        }

        transaction.setAmount(amount);
        transaction.setCategory(category);
        transaction.setDate(request.getDate());
        transaction.setDescription(request.getDescription());

        // Обработка цели
        Goal goal = null;
        if (category.isIncome() && request.getGoalId() != null) {
            goal = goalRepository.findByIdAndUserId(request.getGoalId(), userId)
                    .orElseThrow(() -> new EntityNotFoundException("Цель", request.getGoalId()));

            // Если была предыдущая сумма для цели, сначала отменяем ее
            if (transaction.getGoal() != null && transaction.getAmountContributedToGoal() != null) {
                Goal previousGoal = transaction.getGoal();
                previousGoal.setCurrentAmount(
                        previousGoal.getCurrentAmount().subtract(transaction.getAmountContributedToGoal())
                );
                goalRepository.save(previousGoal);
                log.info("Отменен предыдущий взнос {} к цели '{}'", 
                        transaction.getAmountContributedToGoal(), previousGoal.getName());
            }

            // Проверяем, есть ли amountToGoal в запросе
            BigDecimal amountToGoal = BigDecimal.ZERO; // По умолчанию 0
            
            if (request.getAmountToGoal() != null) {
                amountToGoal = request.getAmountToGoal();
                log.debug("Получена сумма для цели из запроса: {}", amountToGoal);
                
                // Проверяем, что сумма для цели не превышает сумму транзакции
                if (amountToGoal.compareTo(amount) > 0) {
                    throw new InvalidDataException("Сумма для цели не может превышать сумму транзакции");
                }
            } else {
                log.debug("Сумма для цели не указана в запросе, используем 0");
            }

            // Добавляем новую сумму к цели
            goal.setCurrentAmount(goal.getCurrentAmount().add(amountToGoal));
            goalRepository.save(goal);
            transaction.setAmountContributedToGoal(amountToGoal);
            log.info("Добавлен взнос {} к цели '{}' (ID: {})", 
                    amountToGoal, goal.getName(), goal.getId());
        } else {
            // Если цель удалена или это расход, обнуляем связь и сумму
            if (transaction.getGoal() != null && transaction.getAmountContributedToGoal() != null) {
                Goal previousGoal = transaction.getGoal();
                previousGoal.setCurrentAmount(
                        previousGoal.getCurrentAmount().subtract(transaction.getAmountContributedToGoal())
                );
                goalRepository.save(previousGoal);
                log.info("Отменен взнос {} к цели '{}' из-за удаления связи", 
                        transaction.getAmountContributedToGoal(), previousGoal.getName());
            }
            transaction.setAmountContributedToGoal(null);
        }
        transaction.setGoal(goal);

        Transaction updatedTransaction = transactionRepository.save(transaction);
        log.info("Транзакция с ID {} успешно обновлена", id);
        return convertToResponse(updatedTransaction);
    }

    @Transactional
    public void deleteTransaction(Integer id) {
        log.info("Удаление транзакции с ID: {}", id);
        User currentUser = getCurrentUser();
        Integer userId = currentUser.getId();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Транзакция", id));

        if (!transaction.getUser().getId().equals(userId)) {
            throw OperationNotAllowedException.notOwner("транзакция");
        }

        Goal goal = transaction.getGoal();
        BigDecimal amountContributed = transaction.getAmountContributedToGoal() != null
                ? transaction.getAmountContributedToGoal() : BigDecimal.ZERO;

        if (amountContributed.compareTo(BigDecimal.ZERO) > 0 && goal != null) {
            log.info("Откат суммы {} у цели {} при удалении транзакции", amountContributed, goal.getId());
            goal.setCurrentAmount(goal.getCurrentAmount().subtract(amountContributed));
            goal.setCurrentAmount(goal.getCurrentAmount().max(BigDecimal.ZERO));
            goalRepository.save(goal);
            log.info("Текущая сумма цели {} обновлена до {}", goal.getId(), goal.getCurrentAmount());
        }

        if (transaction.getGoal() != null && transaction.getAmountContributedToGoal() != null
                && transaction.getAmountContributedToGoal().compareTo(BigDecimal.ZERO) > 0) {
            Goal goalToUpdate = transaction.getGoal();
            goalToUpdate.setCurrentAmount(goalToUpdate.getCurrentAmount().subtract(transaction.getAmountContributedToGoal()));
            goalRepository.save(goalToUpdate);
            log.info("Отменено добавление {} к цели '{}' (ID: {}) из-за удаления транзакции ID: {}",
                     transaction.getAmountContributedToGoal(), goalToUpdate.getName(), goalToUpdate.getId(), id);
        }

        transactionRepository.delete(transaction);
        log.info("Транзакция с ID {} успешно удалена", id);
    }

    public TransactionResponse getTransactionById(Integer id) {
        User currentUser = getCurrentUser();
        Integer userId = currentUser.getId();
        log.debug("Получение транзакции по ID: {} для пользователя {}", id, currentUser.getEmail());
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Транзакция с ID " + id + " не найдена для пользователя"));
        return convertToResponse(transaction);
    }

    private TransactionResponse convertToResponse(Transaction transaction) {
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
                .amountContributedToGoal(transaction.getAmountContributedToGoal())
                .build();
    }

    private List<String> generateRecommendations(Transaction transaction) {
        log.debug("Генерация рекомендаций для транзакции ID: {}", transaction.getId());
        return Collections.emptyList();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден: " + email));
    }
}