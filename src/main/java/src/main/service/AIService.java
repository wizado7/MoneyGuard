package src.main.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import src.main.dto.ai.AIChatResponse;
import src.main.dto.ai.AnalysisResponse;
import src.main.model.Transaction;
import src.main.model.User;
import src.main.repository.TransactionRepository;
import src.main.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AIService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

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
        // Здесь должна быть логика обработки запроса к ИИ
        // Для примера просто возвращаем заглушку
        
        List<String> advice = new ArrayList<>();
        advice.add("Рекомендуем сократить расходы на развлечения");
        advice.add("Попробуйте увеличить ежемесячные отчисления на цели");
        
        List<String> actions = new ArrayList<>();
        actions.add("Установить лимит на категорию 'Рестораны'");
        actions.add("Создать новую финансовую цель");
        
        return AIChatResponse.builder()
                .message("Я проанализировал ваши финансы и нашел несколько возможностей для оптимизации.")
                .advice(advice)
                .actions(actions)
                .build();
    }

    private List<String> findAnomalies(List<Transaction> transactions) {
        // Здесь должна быть логика поиска аномалий
        // Для примера просто возвращаем заглушку
        
        List<String> anomalies = new ArrayList<>();
        anomalies.add("Расходы на категорию 'Рестораны' выше обычного на 30%");
        anomalies.add("Необычно высокая транзакция в категории 'Одежда'");
        
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
} 