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
    private Integer id;
    private Integer categoryId;
    private String categoryName;
    private BigDecimal amount;
    private String period;
    private String userId;
    private BigDecimal currentUsage;
} 