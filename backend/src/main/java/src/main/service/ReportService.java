package src.main.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import src.main.dto.report.CategoryReport;
import src.main.dto.report.ReportResponse;
import src.main.model.Transaction;
import src.main.model.User;
import src.main.repository.TransactionRepository;
import src.main.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public ReportResponse getReport(LocalDate dateFrom, LocalDate dateTo, String type) {
        User currentUser = getCurrentUser();
        
        // Определяем период отчета
        if (dateFrom == null || dateTo == null) {
            switch (type) {
                case "monthly":
                    dateFrom = LocalDate.now().withDayOfMonth(1);
                    dateTo = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
                    break;
                case "quarterly":
                    int quarter = (LocalDate.now().getMonthValue() - 1) / 3 + 1;
                    dateFrom = LocalDate.now().withMonth((quarter - 1) * 3 + 1).withDayOfMonth(1);
                    dateTo = LocalDate.now().withMonth(quarter * 3).withDayOfMonth(LocalDate.now().withMonth(quarter * 3).lengthOfMonth());
                    break;
                case "yearly":
                    dateFrom = LocalDate.now().withDayOfYear(1);
                    dateTo = LocalDate.now().withDayOfYear(LocalDate.now().lengthOfYear());
                    break;
                default:
                    dateFrom = LocalDate.now().minusMonths(1);
                    dateTo = LocalDate.now();
                    break;
            }
        }
        
        // Получаем транзакции за период
        List<Transaction> transactions = transactionRepository.findByUserAndDateBetweenOrderByDateDesc(
                currentUser, dateFrom, dateTo);
        
        // Рассчитываем доходы и расходы
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        
        // Группируем транзакции по категориям
        Map<String, BigDecimal> categoryAmounts = new HashMap<>();
        
        for (Transaction transaction : transactions) {
            BigDecimal amount = transaction.getAmount();
            
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                income = income.add(amount);
            } else {
                expenses = expenses.add(amount.abs());
            }
            
            String categoryName = transaction.getCategory().getName();
            categoryAmounts.put(
                    categoryName, 
                    categoryAmounts.getOrDefault(categoryName, BigDecimal.ZERO).add(amount.abs())
            );
        }
        
        // Рассчитываем баланс
        BigDecimal balance = income.subtract(expenses);
        
        // Формируем отчет по категориям
        List<CategoryReport> categoryReports = new ArrayList<>();
        
        BigDecimal totalAmount = income.add(expenses);
        
        for (Map.Entry<String, BigDecimal> entry : categoryAmounts.entrySet()) {
            double percentage = 0;
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                percentage = entry.getValue()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalAmount, 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }
            
            categoryReports.add(CategoryReport.builder()
                    .category(entry.getKey())
                    .amount(entry.getValue())
                    .percentage(percentage)
                    .build());
        }
        
        // Сортируем категории по убыванию суммы
        categoryReports = categoryReports.stream()
                .sorted((c1, c2) -> c2.getAmount().compareTo(c1.getAmount()))
                .collect(Collectors.toList());
        
        // Формируем строку периода
        String period = dateFrom.format(DateTimeFormatter.ISO_DATE) + " - " + 
                dateTo.format(DateTimeFormatter.ISO_DATE);
        
        return ReportResponse.builder()
                .period(period)
                .income(income)
                .expenses(expenses)
                .balance(balance)
                .categories(categoryReports)
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }
} 