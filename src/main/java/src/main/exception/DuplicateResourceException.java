package src.main.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BusinessException {
    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
    
    public static DuplicateResourceException forField(String entityName, String fieldName, String value) {
        return new DuplicateResourceException(entityName + " с " + fieldName + " '" + value + "' уже существует");
    }
} 