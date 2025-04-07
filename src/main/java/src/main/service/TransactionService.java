package src.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import src.main.dto.transaction.ImportResponse;
import src.main.dto.transaction.TransactionAIResponse;
import src.main.dto.transaction.TransactionRequest;
import src.main.dto.transaction.TransactionResponse;
import src.main.exception.BusinessException;
import src.main.exception.ConflictException;
import src.main.exception.EntityNotFoundException;
import src.main.exception.InvalidDataException;
import src.main.exception.OperationNotAllowedException;
import src.main.model.Category;
import src.main.model.Goal;
import src.main.model.Limit;
import src.main.model.LimitPeriod;
import src.main.model.Transaction;
import src.main.model.User;
import src.main.repository.CategoryRepository;
import src.main.repository.GoalRepository;
import src.main.repository.LimitRepository;
import src.main.repository.TransactionRepository;
import src.main.repository.UserRepository;
import src.main.util.DateUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    public List<TransactionResponse> getTransactions(LocalDate dateFrom, LocalDate dateTo, String categoryName) {
        User currentUser = getCurrentUser();
        
        List<Transaction> transactions;
        
        if (dateFrom != null && dateTo != null) {
            transactions = transactionRepository.findByUserAndDateBetweenOrderByDateDesc(currentUser, dateFrom, dateTo);
        } else if (categoryName != null && !categoryName.isEmpty()) {
            // Находим категорию по имени
            Category category = categoryRepository.findByNameAndUser(categoryName, currentUser)
                    .orElse(null);
            
            if (category != null) {
                transactions = transactionRepository.findByUserAndCategoryIdOrderByDateDesc(currentUser, category.getId());
            } else {
                transactions = new ArrayList<>();
            }
        } else {
            transactions = transactionRepository.findByUserOrderByDateDesc(currentUser);
        }
        
        return transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    public TransactionAIResponse createTransaction(TransactionRequest request) {
        log.debug("Создание новой транзакции");
        User currentUser = getCurrentUser();
        
        // Проверка категории
        Category category = categoryRepository.findByNameAndUser(request.getCategory(), currentUser)
                .orElseThrow(() -> {
                    log.warn("Категория {} не найдена для пользователя {}", request.getCategory(), currentUser.getEmail());
                    return new EntityNotFoundException("Категория", request.getCategory(), 
                        "Категория '" + request.getCategory() + "' не найдена. Пожалуйста, сначала создайте категорию.");
                });
        
        // Проверка цели, если указана
        Goal goal = null;
        if (request.getGoal_id() != null) {
            goal = goalRepository.findById(request.getGoal_id())
                    .orElseThrow(() -> {
                        log.warn("Цель с ID {} не найдена", request.getGoal_id());
                        return new EntityNotFoundException("Цель", request.getGoal_id());
                    });
            
            // Проверяем, что цель принадлежит текущему пользователю
            if (!goal.getUser().getId().equals(currentUser.getId())) {
                log.warn("Попытка связать транзакцию с чужой целью: {} пользователем: {}", request.getGoal_id(), currentUser.getEmail());
                throw OperationNotAllowedException.notOwner("цель");
            }
        }
        
        Transaction transaction = Transaction.builder()
                .user(currentUser)
                .category(category)
                .goal(goal)
                .amount(request.getAmount())
                .date(request.getDate())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .build();
        
        transactionRepository.save(transaction);
        log.info("Транзакция успешно создана для пользователя: {}", currentUser.getEmail());
        
        // Обновляем сумму цели, если транзакция связана с целью и сумма положительная
        if (goal != null && request.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            goal.setCurrentAmount(goal.getCurrentAmount().add(request.getAmount()));
            goalRepository.save(goal);
            log.info("Обновлена сумма цели {}: {}", goal.getName(), goal.getCurrentAmount());
        }
        
        // Генерируем рекомендации на основе транзакции
        List<String> recommendations = generateRecommendations(transaction);
        
        return TransactionAIResponse.builder()
                .transaction(mapToTransactionResponse(transaction))
                .recommendations(recommendations)
                .build();
    }

    public TransactionResponse updateTransaction(Long id, TransactionRequest request) {
        log.debug("Обновление транзакции с ID: {}", id);
        User currentUser = getCurrentUser();
        
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Транзакция с ID {} не найдена", id);
                    return new EntityNotFoundException("Транзакция", id);
                });
        
        // Проверяем, что транзакция принадлежит текущему пользователю
        if (!transaction.getUser().getId().equals(currentUser.getId())) {
            log.warn("Попытка обновления чужой транзакции: {} пользователем: {}", id, currentUser.getEmail());
            throw OperationNotAllowedException.notOwner("транзакция");
        }
        
        // Проверка категории
        Category category = categoryRepository.findByNameAndUser(request.getCategory(), currentUser)
                .orElseThrow(() -> {
                    log.warn("Категория {} не найдена для пользователя {}", request.getCategory(), currentUser.getEmail());
                    return new EntityNotFoundException("Категория", request.getCategory());
                });
        
        // Проверка цели, если указана
        Goal goal = null;
        if (request.getGoal_id() != null) {
            goal = goalRepository.findById(request.getGoal_id())
                    .orElseThrow(() -> {
                        log.warn("Цель с ID {} не найдена", request.getGoal_id());
                        return new EntityNotFoundException("Цель", request.getGoal_id());
                    });
            
            // Проверяем, что цель принадлежит текущему пользователю
            if (!goal.getUser().getId().equals(currentUser.getId())) {
                log.warn("Попытка связать транзакцию с чужой целью: {} пользователем: {}", request.getGoal_id(), currentUser.getEmail());
                throw OperationNotAllowedException.notOwner("цель");
            }
        }
        
        // Если транзакция была связана с целью и сумма была положительной, обновляем сумму цели
        if (transaction.getGoal() != null && transaction.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            Goal oldGoal = transaction.getGoal();
            oldGoal.setCurrentAmount(oldGoal.getCurrentAmount().subtract(transaction.getAmount()));
            goalRepository.save(oldGoal);
            log.info("Обновлена сумма цели {}: {}", oldGoal.getName(), oldGoal.getCurrentAmount());
        }
        
        transaction.setCategory(category);
        transaction.setGoal(goal);
        transaction.setAmount(request.getAmount());
        transaction.setDate(request.getDate());
        transaction.setDescription(request.getDescription());
        
        transactionRepository.save(transaction);
        log.info("Транзакция успешно обновлена для пользователя: {}", currentUser.getEmail());
        
        // Если транзакция связана с целью и сумма положительная, обновляем сумму цели
        if (goal != null && request.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            goal.setCurrentAmount(goal.getCurrentAmount().add(request.getAmount()));
            goalRepository.save(goal);
            log.info("Обновлена сумма цели {}: {}", goal.getName(), goal.getCurrentAmount());
        }
        
        return mapToTransactionResponse(transaction);
    }

    public void deleteTransaction(Long id) {
        log.debug("Удаление транзакции с ID: {}", id);
        User currentUser = getCurrentUser();
        
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Транзакция с ID {} не найдена", id);
                    return new EntityNotFoundException("Транзакция", id);
                });
        
        // Проверяем, что транзакция принадлежит текущему пользователю
        if (!transaction.getUser().getId().equals(currentUser.getId())) {
            log.warn("Попытка удаления чужой транзакции: {} пользователем: {}", id, currentUser.getEmail());
            throw OperationNotAllowedException.notOwner("транзакция");
        }
        
        // Если транзакция была связана с целью и сумма была положительной, обновляем сумму цели
        if (transaction.getGoal() != null && transaction.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            Goal goal = transaction.getGoal();
            goal.setCurrentAmount(goal.getCurrentAmount().subtract(transaction.getAmount()));
            goalRepository.save(goal);
            log.info("Обновлена сумма цели {}: {}", goal.getName(), goal.getCurrentAmount());
        }
        
        transactionRepository.delete(transaction);
        log.info("Транзакция успешно удалена для пользователя: {}", currentUser.getEmail());
    }

    public ImportResponse importTransactions(MultipartFile file) {
        User currentUser = getCurrentUser();
        
        // Здесь должна быть логика импорта транзакций из файла
        // Для примера просто возвращаем заглушку
        
        return ImportResponse.builder()
                .processed(0)
                .success(0)
                .errors(List.of("Функция импорта находится в разработке"))
                .build();
    }

    private Category findOrCreateCategory(User user, String categoryName) {
        return categoryRepository.findByNameAndUser(categoryName, user)
                .orElseGet(() -> {
                    Category newCategory = Category.builder()
                            .user(user)
                            .name(categoryName)
                            .build();
                    return categoryRepository.save(newCategory);
                });
    }

    private List<String> generateRecommendations(Transaction transaction) {
        // Здесь должна быть логика генерации рекомендаций на основе ИИ
        // Для примера просто возвращаем заглушку
        
        List<String> recommendations = new ArrayList<>();
        
        if (transaction.getAmount().doubleValue() > 1000) {
            recommendations.add("Крупная трата. Рекомендуем пересмотреть бюджет на эту категорию.");
        }
        
        return recommendations;
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

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }
} 