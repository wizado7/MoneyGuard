package src.main.dto.goal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoalPlanResponse {
    private Integer id;
    private String name;
    private String description;
    private BigDecimal target_amount;
    private BigDecimal current_amount;
    private LocalDate target_date;
    private String priority;
    private long days_left;
    private BigDecimal daily_contribution;
    private BigDecimal monthly_contribution;
    private LocalDateTime created_at;
    private String monthly_payment;
    private String optimization_advice;
    private GoalResponse goal;
} 