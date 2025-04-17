package src.main.dto.goal;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalRequest {
    
    @NotBlank(message = "Название цели не может быть пустым")
    private String name;
    
    @NotNull(message = "Целевая сумма должна быть указана")
    @Positive(message = "Целевая сумма должна быть положительной")
    private BigDecimal target_amount;
    
    @NotNull(message = "Текущая сумма должна быть указана")
    @PositiveOrZero(message = "Текущая сумма не может быть отрицательной")
    private BigDecimal current_amount;
    
    @NotNull(message = "Дата достижения цели должна быть указана")
    @Future(message = "Дата достижения цели должна быть в будущем")
    private LocalDate target_date;
    
    @NotNull(message = "Приоритет цели должен быть указан")
    private String priority;
} 