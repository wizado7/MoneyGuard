package src.main.dto.limit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import src.main.dto.category.CategoryResponse;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LimitResponse {
    private Long id;
    private CategoryResponse category;
    private BigDecimal amount;
    private String period;
    private BigDecimal current_spending;
} 