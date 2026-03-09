package Nhom1.Demo_Nhom1.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import Nhom1.Demo_Nhom1.dto.OrderRequest;
import Nhom1.Demo_Nhom1.model.Order;
import Nhom1.Demo_Nhom1.security.AdminAccessChecker;
import Nhom1.Demo_Nhom1.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderApiController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private AdminAccessChecker adminAccessChecker;
    
    @GetMapping
    public ResponseEntity<?> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean all,
            Authentication authentication) {
        String userId = authentication.getName();
        
        if (all) {
            // Return all orders as a list for the orders page
            List<Order> orders = orderService.findAllByUserId(userId);
            return ResponseEntity.ok(orders);
        } else {
            // Return paginated orders for admin
            Page<Order> ordersPage = orderService.findByUserId(userId, PageRequest.of(page, size));
            return ResponseEntity.ok(ordersPage);
        }
    }
    
    /**
     * Tạo đơn hàng mới (checkout)
     * ADMIN không được phép thực hiện hành động này
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request, Authentication authentication) {
        adminAccessChecker.checkNotAdmin("Quản trị viên không được phép tạo đơn hàng. Vui lòng sử dụng tài khoản khách hàng để mua sắm.");
        
        String userId = authentication.getName();
        String username = authentication.getName();
        
        Order order = orderService.createOrder(
                userId, username, request.getShippingAddress(),
                request.getPhone(), request.getPaymentMethod(), request.getNote(),
                request.getSelectedBookIds()
        );
        
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.badRequest().build();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable String id) {
        Order order = orderService.findById(id);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }
    
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable String id, Authentication authentication) {
        String userId = authentication.getName();
        Order order = orderService.findById(id);
        
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (!order.getUserId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        
        if (!"PENDING".equals(order.getStatus())) {
            return ResponseEntity.badRequest().build();
        }
        
        order.setStatus("CANCELLED");
        Order updatedOrder = orderService.save(order);
        return ResponseEntity.ok(updatedOrder);
    }
}
