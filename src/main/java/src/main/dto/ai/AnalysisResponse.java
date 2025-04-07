package src.main.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisResponse {
    private String period;
    private Map<String, BigDecimal> categories;
    private List<String> anomalies;
    private Forecast forecast;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Forecast {
        private BigDecimal balance;
        private String risk_level;
    }
} 