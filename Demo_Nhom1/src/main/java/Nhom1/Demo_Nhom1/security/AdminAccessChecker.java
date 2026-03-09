package Nhom1.Demo_Nhom1.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import Nhom1.Demo_Nhom1.exception.AdminCannotAddToCartException;

/**
 * Utility class để kiểm tra quyền truy cập của Admin
 * Chặn các hành động mua sắm nếu user có role ADMIN
 */
@Component
public class AdminAccessChecker {
    
    /**
     * Kiểm tra xem user hiện tại có phải là admin không
     * Nếu là admin, throw AdminCannotAddToCartException
     * 
     * @param message Thông báo lỗi tùy chỉnh
     * @throws AdminCannotAddToCartException nếu user là admin
     */
    public void checkNotAdmin(String message) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated()) {
            // Kiểm tra xem user có role ADMIN không
            boolean isAdmin = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role -> role.equals("ROLE_ADMIN"));
            
            if (isAdmin) {
                throw new AdminCannotAddToCartException(message);
            }
        }
    }
    
    /**
     * Kiểm tra xem user hiện tại có phải là admin không với message mặc định
     * 
     * @throws AdminCannotAddToCartException nếu user là admin
     */
    public void checkNotAdmin() {
        checkNotAdmin("Quản trị viên không được phép thực hiện hành động này");
    }
    
    /**
     * Kiểm tra xem user hiện tại có phải là admin không
     * 
     * @return true nếu user là admin, false nếu không
     */
    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role -> role.equals("ROLE_ADMIN"));
        }
        
        return false;
    }
}
