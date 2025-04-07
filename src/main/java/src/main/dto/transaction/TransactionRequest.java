package src.main.dto.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
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
public class TransactionRequest {
    
    @NotNull(message = "Сумма не может быть пустой")
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0")
    private BigDecimal amount;
    
    @NotBlank(message = "Категория не может быть пустой")
    private String category;
    
    @NotNull(message = "Дата не может быть пустой")
    @PastOrPresent(message = "Дата не может быть в будущем")
    private LocalDate date;
    
    @Size(max = 255, message = "Описание не может быть длиннее 255 символов")
    private String description;
    
    private Long goal_id;
} 