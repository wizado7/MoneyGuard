package src.main.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class EntityNotFoundException extends RuntimeException {
    
    public EntityNotFoundException(String entityName, Object entityId) {
        super(String.format("%s с идентификатором %s не найден", entityName, entityId));
    }
    
    public EntityNotFoundException(String entityName, Object entityId, String customMessage) {
        super(customMessage);
    }
    
    public EntityNotFoundException(String message) {
        super(message);
    }
} 