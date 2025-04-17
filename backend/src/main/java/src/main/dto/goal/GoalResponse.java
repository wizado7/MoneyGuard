package src.main.dto.goal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoalResponse {
    private Integer id;
    private String name;
    private BigDecimal target_amount;
    private BigDecimal current_amount;
    private LocalDate target_date;
    private String priority;
} 