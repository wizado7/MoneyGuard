package src.main.dto.limit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LimitRequest {
    
    @NotNull(message = "ID категории не может быть пустым")
    @Positive(message = "ID категории должен быть положительным числом")
    private Long category_id;
    
    @NotNull(message = "Сумма не может быть пустой")
    @Positive(message = "Сумма должна быть положительным числом")
    private BigDecimal amount;
    
    @NotBlank(message = "Период не может быть пустым")
    @Pattern(regexp = "DAILY|WEEKLY|MONTHLY|YEARLY", message = "Период должен быть одним из: DAILY, WEEKLY, MONTHLY, YEARLY")
    private String period;
} 