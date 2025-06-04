package src.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import src.main.dto.transaction.TransactionAIResponse;
import src.main.dto.transaction.TransactionRequest;
import src.main.dto.transaction.TransactionResponse;
import src.main.exception.EntityNotFoundException;
import src.main.exception.InvalidDataException;
import src.main.exception.OperationNotAllowedException;
import src.main.exception.UserNotFoundException;
import src.main.model.*;
import src.main.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
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
    private final HttpGeminiService httpGeminiService;
    private final ChatHistoryRepository chatHistoryRepository;

    public List<TransactionResponse> getTransactions(LocalDate dateFrom, LocalDate dateTo, String categoryName) {
        User currentUser = getCurrentUser();
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
        
        List<String> recommendations = new ArrayList<>();
        
        try {
            // 1. Сначала получаем локальные рекомендации (быстрые)
            List<String> localRecommendations = generateLocalRecommendations(transaction);
            recommendations.addAll(localRecommendations);
            
            // 2. Затем получаем AI рекомендации через Gemini API
            List<String> aiRecommendations = generateAIRecommendations(transaction);
            recommendations.addAll(aiRecommendations);
            
        } catch (Exception e) {
            log.error("Ошибка при генерации рекомендаций: {}", e.getMessage(), e);
            // Возвращаем базовые рекомендации в случае ошибки
            recommendations.add("Продолжайте отслеживать свои финансы для лучшего контроля над бюджетом");
            recommendations.add("Рассмотрите возможность создания финансовых целей");
        }
        
        // Ограничиваем общее количество рекомендаций
        if (recommendations.size() > 5) {
            recommendations = recommendations.subList(0, 5);
        }
        
        log.debug("Сгенерировано {} рекомендаций для транзакции", recommendations.size());
        return recommendations;
    }

    private List<String> generateLocalRecommendations(Transaction transaction) {
        List<String> recommendations = new ArrayList<>();
        User user = transaction.getUser();
        Category category = transaction.getCategory();
        BigDecimal amount = transaction.getAmount().abs();
        
        try {
            // Анализируем паттерны трат пользователя за последний месяц
            LocalDate dateFrom = LocalDate.now().minusMonths(1);
            LocalDate dateTo = LocalDate.now();
            List<Transaction> recentTransactions = transactionRepository.findByUserAndDateBetweenOrderByDateDesc(
                    user, dateFrom, dateTo);
            
            // Рекомендации для доходов
            if (category.isIncome()) {
                recommendations.add("Отлично! Новый доход поможет укрепить ваше финансовое положение");
                
                // Рекомендации по инвестированию части дохода
                if (amount.compareTo(BigDecimal.valueOf(10000)) > 0) {
                    recommendations.add("Рассмотрите возможность инвестирования 10-15% от этого дохода");
                }
                
                // Рекомендация создать подушку безопасности
                if (transaction.getGoal() == null) {
                    recommendations.add("Рекомендуем направить часть дохода на создание финансовой подушки безопасности");
                }
            } else {
                // Рекомендации для расходов
                
                // Анализируем лимиты категории
                Optional<Limit> categoryLimit = limitRepository.findByUserAndCategory(user, category);
                if (categoryLimit.isPresent()) {
                    BigDecimal limitAmount = categoryLimit.get().getAmount();
                    
                    // Вычисляем сколько потрачено в этой категории за текущий месяц
                    LocalDate currentMonthStart = LocalDate.now().withDayOfMonth(1);
                    List<Transaction> monthlyExpenses = transactionRepository.findByUserAndCategoryIdAndDateBetweenOrderByDateDesc(
                            user, category.getId(), currentMonthStart, LocalDate.now());
                    
                    BigDecimal totalSpent = monthlyExpenses.stream()
                            .map(Transaction::getAmount)
                            .map(BigDecimal::abs)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    double spentPercentage = totalSpent.divide(limitAmount, 4, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();
                    
                    if (spentPercentage > 80) {
                        recommendations.add("Внимание! Вы потратили " + String.format("%.1f", spentPercentage) + 
                                "% от лимита в категории '" + category.getName() + "'");
                    } else if (spentPercentage > 50) {
                        recommendations.add("Вы потратили " + String.format("%.1f", spentPercentage) + 
                                "% от лимита в категории '" + category.getName() + "'. Контролируйте расходы");
                    }
                }
                
                // Анализируем размер траты относительно среднего
                if (recentTransactions.size() > 3) {
                    BigDecimal averageExpense = recentTransactions.stream()
                            .filter(t -> !t.getCategory().isIncome())
                            .map(Transaction::getAmount)
                            .map(BigDecimal::abs)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(recentTransactions.size()), 2, java.math.RoundingMode.HALF_UP);
                    
                    if (amount.compareTo(averageExpense.multiply(BigDecimal.valueOf(2))) > 0) {
                        recommendations.add("Эта трата значительно превышает ваши обычные расходы. Убедитесь, что это запланированная покупка");
                    }
                }
            }
            
            // Добавляем мотивационные сообщения если нет других рекомендаций
            if (recommendations.isEmpty()) {
                if (category.isIncome()) {
                    recommendations.add("Продолжайте в том же духе! Регулярные доходы - основа финансовой стабильности");
                } else {
                    recommendations.add("Хорошо, что вы отслеживаете свои расходы! Это первый шаг к финансовой грамотности");
                }
            }
            
        } catch (Exception e) {
            log.error("Ошибка при генерации локальных рекомендаций: {}", e.getMessage(), e);
        }
        
        return recommendations;
    }

    private List<String> generateAIRecommendations(Transaction transaction) {
        List<String> aiRecommendations = new ArrayList<>();
        
        try {
            User user = transaction.getUser();
            Category category = transaction.getCategory();
            BigDecimal amount = transaction.getAmount().abs();
            
            // Строим контекст для AI запроса
            String aiContext = buildTransactionContext(transaction, user);
            
            // Формируем запрос к Gemini API
            String prompt = String.format(
                "Пользователь только что %s %s рублей в категории '%s'.\n\n" +
                "Контекст пользователя:\n%s\n\n" +
                "Дай 1-2 конкретных совета по управлению финансами, учитывая эту транзакцию. " +
                "Ответ должен быть полезным и практичным. Каждый совет начинай с новой строки и используй эмодзи.",
                category.isIncome() ? "получил доход в размере" : "потратил",
                amount,
                category.getName(),
                aiContext
            );
            
            // Получаем ответ от Gemini API через прокси
            String aiResponse = httpGeminiService.generateFinancialAdvice(prompt);
            
            // Парсим ответ AI
            List<String> parsedRecommendations = parseAIRecommendations(aiResponse);
            aiRecommendations.addAll(parsedRecommendations);
            
            // Сохраняем AI рекомендации в чат-историю
            saveRecommendationsToChat(user, transaction, aiResponse);
            
            log.debug("Получено {} AI рекомендаций для транзакции", aiRecommendations.size());
            
        } catch (Exception e) {
            log.warn("Не удалось получить AI рекомендации: {}", e.getMessage());
            // Добавляем fallback рекомендацию
            aiRecommendations.add("Рекомендуем регулярно анализировать свои траты для лучшего финансового планирования");
        }
        
        return aiRecommendations;
    }

    private String buildTransactionContext(Transaction transaction, User user) {
        try {
            StringBuilder context = new StringBuilder();
            
            // Основная информация о пользователе
            context.append("Пользователь: ").append(user.getEmail()).append("\n");
            
            // Анализ последних транзакций за месяц
            LocalDate dateFrom = LocalDate.now().minusMonths(1);
            LocalDate dateTo = LocalDate.now();
            List<Transaction> recentTransactions = transactionRepository.findByUserAndDateBetweenOrderByDateDesc(
                    user, dateFrom, dateTo);
            
            if (!recentTransactions.isEmpty()) {
                // Подсчитываем доходы и расходы
                BigDecimal totalIncome = recentTransactions.stream()
                        .filter(t -> t.getCategory().isIncome())
                        .map(Transaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal totalExpenses = recentTransactions.stream()
                        .filter(t -> !t.getCategory().isIncome())
                        .map(Transaction::getAmount)
                        .map(BigDecimal::abs)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                context.append("Статистика за последний месяц:\n");
                context.append("- Общий доход: ").append(totalIncome).append(" руб.\n");
                context.append("- Общие расходы: ").append(totalExpenses).append(" руб.\n");
                context.append("- Баланс: ").append(totalIncome.subtract(totalExpenses)).append(" руб.\n");
                
                // Топ-3 категории расходов
                var expensesByCategory = recentTransactions.stream()
                        .filter(t -> !t.getCategory().isIncome())
                        .collect(Collectors.groupingBy(
                                t -> t.getCategory().getName(),
                                Collectors.reducing(BigDecimal.ZERO, 
                                        t -> t.getAmount().abs(), 
                                        BigDecimal::add)));
                
                context.append("Топ категории расходов:\n");
                expensesByCategory.entrySet().stream()
                        .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                        .limit(3)
                        .forEach(entry -> context.append("- ").append(entry.getKey())
                                .append(": ").append(entry.getValue()).append(" руб.\n"));
            }
            
            return context.toString();
            
        } catch (Exception e) {
            log.error("Ошибка при построении контекста транзакции: {}", e.getMessage(), e);
            return "Базовая информация о пользователе недоступна.";
        }
    }

    private List<String> parseAIRecommendations(String aiResponse) {
        List<String> recommendations = new ArrayList<>();
        
        try {
            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                return recommendations;
            }
            
            // Разбиваем ответ на строки
            String[] lines = aiResponse.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                
                // Пропускаем пустые строки
                if (line.isEmpty()) {
                    continue;
                }
                
                // Очищаем от маркеров списка
                line = line.replaceAll("^[0-9]+[.)\\s]*", "")
                          .replaceAll("^[-*•]\\s*", "")
                          .trim();
                
                // Добавляем только содержательные рекомендации
                if (line.length() > 10 && !line.toLowerCase().startsWith("конечно") && 
                    !line.toLowerCase().startsWith("хорошо")) {
                    recommendations.add(line);
                }
            }
            
            // Если не удалось разобрать по строкам, разбиваем по предложениям
            if (recommendations.isEmpty()) {
                String[] sentences = aiResponse.split("[.!?]+");
                for (String sentence : sentences) {
                    sentence = sentence.trim();
                    if (sentence.length() > 15) {
                        recommendations.add(sentence);
                        if (recommendations.size() >= 2) break; // Ограничиваем 2 рекомендациями
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Ошибка при парсинге AI рекомендаций: {}", e.getMessage(), e);
        }
        
        return recommendations;
    }

    private void saveRecommendationsToChat(User user, Transaction transaction, String aiResponse) {
        try {
            // Формируем сообщение пользователя о транзакции
            String userMessage = String.format(
                "Создана транзакция: %s %.2f руб. в категории '%s'%s",
                transaction.getCategory().isIncome() ? "доход" : "расход",
                transaction.getAmount().abs(),
                transaction.getCategory().getName(),
                transaction.getDescription() != null && !transaction.getDescription().isEmpty() 
                    ? " (" + transaction.getDescription() + ")" : ""
            );
            
            // Сохраняем сообщение пользователя
            ChatHistory userHistoryEntry = ChatHistory.builder()
                    .user(user)
                    .message(userMessage)
                    .response("")
                    .role(ChatHistory.MessageRole.USER)
                    .build();
            chatHistoryRepository.save(userHistoryEntry);
            
            // Сохраняем ответ AI с рекомендациями
            String assistantMessage = "Рекомендации по транзакции:\n\n" + aiResponse;
            
            ChatHistory assistantHistoryEntry = ChatHistory.builder()
                    .user(user)
                    .message("")
                    .response(assistantMessage)
                    .role(ChatHistory.MessageRole.ASSISTANT)
                    .build();
            chatHistoryRepository.save(assistantHistoryEntry);
            
            log.debug("AI рекомендации сохранены в чат-историю для пользователя {}", user.getEmail());
            
        } catch (Exception e) {
            log.warn("Не удалось сохранить рекомендации в чат-историю: {}", e.getMessage());
            // Не прерываем работу из-за ошибки сохранения в чат
        }
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден: " + email));
    }
}