package src.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import src.main.dto.ai.AIChatResponse;
import src.main.dto.ai.AnalysisResponse;
import src.main.model.ChatHistory;
import src.main.model.Transaction;
import src.main.model.User;
import src.main.repository.ChatHistoryRepository;
import src.main.repository.TransactionRepository;
import src.main.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final HttpGeminiService httpGeminiService;

    public AnalysisResponse getAnalysis() {
        User currentUser = getCurrentUser();
        
        // Определяем период анализа (последние 3 месяца)
        LocalDate dateFrom = LocalDate.now().minusMonths(3);
        LocalDate dateTo = LocalDate.now();
        
        // Получаем транзакции за период
        List<Transaction> transactions = transactionRepository.findByUserAndDateBetweenOrderByDateDesc(
                currentUser, dateFrom, dateTo);
        
        // Группируем транзакции по категориям
        Map<String, BigDecimal> categoryAmounts = new HashMap<>();
        
        for (Transaction transaction : transactions) {
            String categoryName = transaction.getCategory().getName();
            BigDecimal amount = transaction.getAmount().abs();
            
            categoryAmounts.put(
                    categoryName, 
                    categoryAmounts.getOrDefault(categoryName, BigDecimal.ZERO).add(amount)
            );
        }
        
        // Находим аномалии (для примера - категории с расходами выше среднего)
        List<String> anomalies = findAnomalies(transactions);
        
        // Формируем прогноз
        AnalysisResponse.Forecast forecast = createForecast(transactions);
        
        // Формируем строку периода
        String period = dateFrom.format(DateTimeFormatter.ISO_DATE) + " - " + 
                dateTo.format(DateTimeFormatter.ISO_DATE);
        
        return AnalysisResponse.builder()
                .period(period)
                .categories(categoryAmounts)
                .anomalies(anomalies)
                .forecast(forecast)
                .build();
    }

    public AIChatResponse chat(String message, MultipartFile image) {
        log.debug("Обработка AI чата для сообщения: {}", message);
        
        try {
            User currentUser = getCurrentUser();
            
            // Получаем историю последних 10 сообщений для контекста
            org.springframework.data.domain.Pageable pageable = PageRequest.of(0, 10);
            List<ChatHistory> recentHistory = chatHistoryRepository.findLastNMessages(currentUser, pageable);
            
            // Получаем контекст пользователя для более персонализированного ответа
            String userContext = buildUserContext();
            
            // Формируем полный запрос с контекстом истории
            String chatHistoryContext = buildChatHistoryContext(recentHistory);
            String fullMessage = String.format(
                "Контекст пользователя:\n%s\n\nИстория переписки:\n%s\n\nТекущий вопрос пользователя: %s\n\n" +
                "Дай практический совет по личным финансам на основе этой информации и контекста предыдущих сообщений.",
                userContext, chatHistoryContext, message
            );
            
            // Получаем ответ от Gemini API
            String aiResponse = httpGeminiService.generateFinancialAdvice(fullMessage);
            
            // Сохраняем пользовательское сообщение в историю
            ChatHistory userHistoryEntry = ChatHistory.builder()
                    .user(currentUser)
                    .message(message)
                    .response("")
                    .role(ChatHistory.MessageRole.USER)
                    .build();
            chatHistoryRepository.save(userHistoryEntry);
            
            // Сохраняем ответ ассистента в историю
            ChatHistory assistantHistoryEntry = ChatHistory.builder()
                    .user(currentUser)
                    .message("")
                    .response(aiResponse)
                    .role(ChatHistory.MessageRole.ASSISTANT)
                    .build();
            chatHistoryRepository.save(assistantHistoryEntry);
            
            // Парсим ответ и формируем структурированный результат
            List<String> advice = parseAdviceFromResponse(aiResponse);
            List<String> actions = generateActionsFromAdvice(advice);
            
            return AIChatResponse.builder()
                    .message(aiResponse)
                    .advice(advice)
                    .actions(actions)
                    .build();
                    
        } catch (Exception e) {
            log.error("Ошибка при обработке AI чата: {}", e.getMessage(), e);
            
            // Возвращаем fallback ответ
            List<String> fallbackAdvice = Arrays.asList(
                "Рекомендуем вести регулярный учет доходов и расходов",
                "Создайте финансовую подушку безопасности",
                "Планируйте крупные покупки заранее"
            );
            
            List<String> fallbackActions = Arrays.asList(
                "Установить лимиты на категории расходов",
                "Создать финансовую цель"
            );
            
            return AIChatResponse.builder()
                    .message("Извините, произошла временная ошибка. Вот общие рекомендации по управлению финансами.")
                    .advice(fallbackAdvice)
                    .actions(fallbackActions)
                    .build();
        }
    }

    private List<String> findAnomalies(List<Transaction> transactions) {
        List<String> anomalies = new ArrayList<>();
        
        if (transactions.isEmpty()) {
            return anomalies;
        }
        
        try {
            // Группируем транзакции по категориям
            Map<String, List<Transaction>> transactionsByCategory = transactions.stream()
                    .filter(t -> t.getCategory() != null)
                    .collect(Collectors.groupingBy(t -> t.getCategory().getName()));
            
            // Анализируем каждую категорию
            for (Map.Entry<String, List<Transaction>> entry : transactionsByCategory.entrySet()) {
                String category = entry.getKey();
                List<Transaction> categoryTransactions = entry.getValue();
                
                // Вычисляем среднюю сумму для категории
                BigDecimal averageAmount = categoryTransactions.stream()
                        .map(Transaction::getAmount)
                        .map(BigDecimal::abs)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(categoryTransactions.size()), 2, RoundingMode.HALF_UP);
                
                // Ищем транзакции, значительно превышающие среднее
                for (Transaction transaction : categoryTransactions) {
                    BigDecimal amount = transaction.getAmount().abs();
                    if (amount.compareTo(averageAmount.multiply(BigDecimal.valueOf(2))) > 0) {
                        anomalies.add(String.format("Необычно высокая транзакция в категории '%s': %.2f руб.", 
                                category, amount));
                    }
                }
            }
            
            // Если аномалий не найдено, добавляем общие наблюдения
            if (anomalies.isEmpty()) {
                BigDecimal totalExpenses = transactions.stream()
                        .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                        .map(Transaction::getAmount)
                        .map(BigDecimal::abs)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                if (totalExpenses.compareTo(BigDecimal.valueOf(50000)) > 0) {
                    anomalies.add("Общая сумма расходов довольно высокая");
                }
            }
            
        } catch (Exception e) {
            log.error("Ошибка при поиске аномалий: {}", e.getMessage(), e);
            anomalies.add("Не удалось проанализировать аномалии в транзакциях");
        }
        
        return anomalies;
    }

    private AnalysisResponse.Forecast createForecast(List<Transaction> transactions) {
        // Здесь должна быть логика создания прогноза
        // Для примера просто возвращаем заглушку
        
        return AnalysisResponse.Forecast.builder()
                .balance(BigDecimal.valueOf(15000))
                .risk_level("medium")
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    private String buildChatHistoryContext(List<ChatHistory> recentHistory) {
        if (recentHistory == null || recentHistory.isEmpty()) {
            return "Это первое сообщение пользователя в данной сессии.";
        }
        
        StringBuilder historyContext = new StringBuilder();
        historyContext.append("Предыдущие сообщения (от новых к старым):\n");
        
        try {
            // Обращаем порядок, чтобы показать от старых к новым
            Collections.reverse(recentHistory);
            
            for (ChatHistory entry : recentHistory) {
                if (entry.getRole() == ChatHistory.MessageRole.USER && 
                    entry.getMessage() != null && !entry.getMessage().trim().isEmpty()) {
                    historyContext.append("Пользователь: ").append(entry.getMessage()).append("\n");
                }
                
                if (entry.getRole() == ChatHistory.MessageRole.ASSISTANT && 
                    entry.getResponse() != null && !entry.getResponse().trim().isEmpty()) {
                    // Сокращаем длинные ответы для контекста
                    String response = entry.getResponse();
                    if (response.length() > 200) {
                        response = response.substring(0, 200) + "...";
                    }
                    historyContext.append("Ассистент: ").append(response).append("\n");
                }
                historyContext.append("---\n");
            }
        } catch (Exception e) {
            log.error("Ошибка при построении контекста истории: {}", e.getMessage(), e);
            return "Не удалось получить историю предыдущих сообщений.";
        }
        
        return historyContext.toString();
    }

    private String buildUserContext() {
        try {
            User currentUser = getCurrentUser();
            LocalDate dateFrom = LocalDate.now().minusMonths(1);
            LocalDate dateTo = LocalDate.now();
            
            List<Transaction> recentTransactions = transactionRepository.findByUserAndDateBetweenOrderByDateDesc(
                    currentUser, dateFrom, dateTo);
            
            if (recentTransactions.isEmpty()) {
                return "Пользователь: " + currentUser.getEmail() + "\nТранзакций за последний месяц не найдено.";
            }
            
            StringBuilder context = new StringBuilder();
            context.append("Пользователь: ").append(currentUser.getEmail()).append("\n");
            context.append("Анализ за период: ").append(dateFrom).append(" - ").append(dateTo).append("\n\n");
            
            // Подсчитываем общие доходы и расходы
            BigDecimal totalIncome = BigDecimal.ZERO;
            BigDecimal totalExpenses = BigDecimal.ZERO;
            Map<String, BigDecimal> categoryExpenses = new HashMap<>();
            
            for (Transaction transaction : recentTransactions) {
                BigDecimal amount = transaction.getAmount();
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    totalIncome = totalIncome.add(amount);
                } else {
                    totalExpenses = totalExpenses.add(amount.abs());
                    String categoryName = transaction.getCategory() != null ? 
                            transaction.getCategory().getName() : "Без категории";
                    categoryExpenses.put(categoryName, 
                            categoryExpenses.getOrDefault(categoryName, BigDecimal.ZERO).add(amount.abs()));
                }
            }
            
            context.append("Общий доход: ").append(totalIncome).append(" руб.\n");
            context.append("Общие расходы: ").append(totalExpenses).append(" руб.\n");
            context.append("Баланс: ").append(totalIncome.subtract(totalExpenses)).append(" руб.\n\n");
            
            context.append("Расходы по категориям:\n");
            categoryExpenses.entrySet().stream()
                    .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .forEach(entry -> context.append("- ").append(entry.getKey())
                            .append(": ").append(entry.getValue()).append(" руб.\n"));
            
            return context.toString();
            
        } catch (Exception e) {
            log.error("Ошибка при построении контекста пользователя: {}", e.getMessage(), e);
            return "Контекст пользователя недоступен из-за ошибки.";
        }
    }

    private List<String> parseAdviceFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return Arrays.asList("Не удалось получить рекомендации от AI");
        }
        
        List<String> advice = new ArrayList<>();
        
        try {
            // Разбиваем ответ на строки и ищем пункты с советами
            String[] lines = response.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                
                // Ищем строки, которые начинаются с маркеров списка или цифр
                if (line.matches("^[0-9]+[.)\\s].*") || 
                    line.matches("^[-*•]\\s.*") ||
                    (line.length() > 10 && (line.contains("рекомендую") || line.contains("совет")))) {
                    
                    // Очищаем от маркеров
                    String cleanAdvice = line.replaceAll("^[0-9]+[.)\\s]*", "")
                                           .replaceAll("^[-*•]\\s*", "")
                                           .trim();
                    
                    if (cleanAdvice.length() > 5) { // Минимальная длина для валидного совета
                        advice.add(cleanAdvice);
                    }
                }
            }
            
            // Если не удалось найти структурированные советы, разбиваем на предложения
            if (advice.isEmpty()) {
                String[] sentences = response.split("[.!?]+");
                for (String sentence : sentences) {
                    sentence = sentence.trim();
                    if (sentence.length() > 20 && 
                        (sentence.contains("рекомендую") || sentence.contains("стоит") || 
                         sentence.contains("можно") || sentence.contains("нужно"))) {
                        advice.add(sentence);
                    }
                }
            }
            
            // Ограничиваем количество советов
            if (advice.size() > 5) {
                advice = advice.subList(0, 5);
            }
            
        } catch (Exception e) {
            log.error("Ошибка при парсинге советов: {}", e.getMessage(), e);
            advice.add("Ошибка при обработке рекомендаций AI");
        }
        
        // Если ничего не найдено, возвращаем базовые советы
        if (advice.isEmpty()) {
            advice.addAll(Arrays.asList(
                "Ведите регулярный учет доходов и расходов",
                "Создайте финансовую подушку безопасности на 3-6 месяцев",
                "Планируйте крупные покупки заранее"
            ));
        }
        
        return advice;
    }

    private List<String> generateActionsFromAdvice(List<String> advice) {
        List<String> actions = new ArrayList<>();
        
        try {
            for (String adviceItem : advice) {
                String lowerAdvice = adviceItem.toLowerCase();
                
                // Анализируем содержание совета и предлагаем конкретные действия
                if (lowerAdvice.contains("бюджет") || lowerAdvice.contains("планирование")) {
                    actions.add("Настроить бюджет в приложении");
                } else if (lowerAdvice.contains("категори") || lowerAdvice.contains("расход")) {
                    actions.add("Установить лимиты на категории расходов");
                } else if (lowerAdvice.contains("накопления") || lowerAdvice.contains("сбережения")) {
                    actions.add("Создать цель накоплений");
                } else if (lowerAdvice.contains("долг") || lowerAdvice.contains("кредит")) {
                    actions.add("Запланировать погашение долгов");
                } else if (lowerAdvice.contains("инвестиции") || lowerAdvice.contains("вложения")) {
                    actions.add("Изучить инвестиционные возможности");
                } else if (lowerAdvice.contains("экономия") || lowerAdvice.contains("сокращ")) {
                    actions.add("Проанализировать возможности экономии");
                } else {
                    // Общее действие для неклассифицированных советов
                    actions.add("Применить рекомендацию в финансовом планировании");
                }
            }
            
            // Удаляем дубликаты
            actions = actions.stream().distinct().collect(Collectors.toList());
            
            // Добавляем базовые действия если список пуст
            if (actions.isEmpty()) {
                actions.addAll(Arrays.asList(
                    "Настроить уведомления о тратах",
                    "Создать финансовую цель",
                    "Просмотреть отчет по расходам"
                ));
            }
            
        } catch (Exception e) {
            log.error("Ошибка при генерации действий: {}", e.getMessage(), e);
            actions.add("Ошибка при формировании рекомендуемых действий");
        }
        
        return actions;
    }
}