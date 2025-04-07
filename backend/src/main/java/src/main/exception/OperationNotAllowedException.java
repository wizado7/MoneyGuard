package src.main.exception;

import org.springframework.http.HttpStatus;

public class OperationNotAllowedException extends BusinessException {
    
    public OperationNotAllowedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
    
    public static OperationNotAllowedException notOwner(String entityName) {
        return new OperationNotAllowedException("У вас нет прав для выполнения операции с " + entityName);
    }
    
    public static OperationNotAllowedException readOnly(String entityName) {
        return new OperationNotAllowedException(entityName + " доступен только для чтения");
    }
} 