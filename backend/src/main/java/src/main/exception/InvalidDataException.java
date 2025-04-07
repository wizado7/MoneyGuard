package src.main.exception;

import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

public class InvalidDataException extends BusinessException {
    
    private final Map<String, String> errors;
    
    public InvalidDataException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
        this.errors = new HashMap<>();
    }
    
    public InvalidDataException(String message, Map<String, String> errors) {
        super(message, HttpStatus.BAD_REQUEST);
        this.errors = errors;
    }
    
    public InvalidDataException addError(String field, String message) {
        this.errors.put(field, message);
        return this;
    }
    
    public Map<String, String> getErrors() {
        return errors;
    }
} 