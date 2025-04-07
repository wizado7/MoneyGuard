package src.main.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedException extends BusinessException {
    public AccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
    
    public static AccessDeniedException forResource(String resourceName) {
        return new AccessDeniedException("У вас нет прав для доступа к " + resourceName);
    }
} 