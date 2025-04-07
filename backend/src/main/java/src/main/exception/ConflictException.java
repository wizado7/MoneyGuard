package src.main.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessException {
    
    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
    
    public static ConflictException entityExists(String entityName, String fieldName, Object value) {
        return new ConflictException(entityName + " с " + fieldName + " '" + value + "' уже существует");
    }
    
    public static ConflictException dependencyExists(String entityName, String dependentEntity) {
        return new ConflictException("Невозможно удалить " + entityName + ", так как существуют связанные " + dependentEntity);
    }
} 