package Nhom1.Demo_Nhom1.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import Nhom1.Demo_Nhom1.dto.CartRequest;
import Nhom1.Demo_Nhom1.model.Cart;
import Nhom1.Demo_Nhom1.security.AdminAccessChecker;
import Nhom1.Demo_Nhom1.service.CartService;

@RestController
@RequestMapping("/api/cart")
public class CartApiController {
    
    @Autowired
    private CartService cartService;
    
    @Autowired
    private AdminAccessChecker adminAccessChecker;
    
    @GetMapping
    public ResponseEntity<Cart> getCart(Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }
    
    /**
     * Thêm sản phẩm vào giỏ hàng
     * ADMIN không được phép thực hiện hành động này
     */
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody CartRequest request, Authentication authentication) {
        adminAccessChecker.checkNotAdmin("Quản trị viên không được phép thêm sản phẩm vào giỏ hàng. Vui lòng sử dụng tài khoản khách hàng để mua sắm.");
        
        String userId = authentication.getName();
        Cart cart = cartService.addToCart(userId, request.getBookId(), request.getQuantity());
        
        if (cart == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Không đủ hàng trong kho"));
        }
        
        return ResponseEntity.ok(cart);
    }
    
    @DeleteMapping("/remove/{bookId}")
    public ResponseEntity<Cart> removeFromCart(@PathVariable Long bookId, Authentication authentication) {
        adminAccessChecker.checkNotAdmin("Quản trị viên không được phép xóa sản phẩm khỏi giỏ hàng.");
        
        String userId = authentication.getName();
        return ResponseEntity.ok(cartService.removeFromCart(userId, bookId));
    }
    
    @PutMapping("/update")
    public ResponseEntity<?> updateQuantity(@RequestBody CartRequest request, Authentication authentication) {
        adminAccessChecker.checkNotAdmin("Quản trị viên không được phép cập nhật giỏ hàng.");
        
        String userId = authentication.getName();
        Cart cart = cartService.updateQuantity(userId, request.getBookId(), request.getQuantity());
        
        if (cart == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Không đủ hàng trong kho"));
        }
        
        return ResponseEntity.ok(cart);
    }
    
    // Inner class for error response
    private static class ErrorResponse {
        private String message;
        
        public ErrorResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
