package Nhom1.Demo_Nhom1.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AdminCannotAddToCartException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleAdminCannotAddToCart(AdminCannotAddToCartException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "ADMIN_CANNOT_ADD_TO_CART");
        response.put("message", ex.getMessage());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
}
