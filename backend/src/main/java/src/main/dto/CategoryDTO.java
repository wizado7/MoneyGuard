package src.main.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryDTO {
    private Integer id;

    @NotBlank(message = "Название категории не может быть пустым")
    private String name;

    // Поля необязательны
    private String iconName;
    private String colorHex;

    @NotNull(message = "Тип категории (доход/расход) должен быть указан")
    private Boolean isIncome;

    private Integer parentId;
    private String userId; // Может быть null для общих категорий
} 