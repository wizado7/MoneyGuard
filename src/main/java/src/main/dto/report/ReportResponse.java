package src.main.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportResponse {
    private String period;
    private BigDecimal income;
    private BigDecimal expenses;
    private BigDecimal balance;
    private List<CategoryReport> categories;
} 