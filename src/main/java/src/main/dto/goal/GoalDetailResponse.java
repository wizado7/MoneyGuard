package src.main.dto.goal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import src.main.dto.transaction.TransactionResponse;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoalDetailResponse {
    private GoalResponse goal;
    private List<TransactionResponse> transactions;
    private double progress;
} 