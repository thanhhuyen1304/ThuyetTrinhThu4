package Nhom1.Demo_Nhom1.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import Nhom1.Demo_Nhom1.model.Order;
import Nhom1.Demo_Nhom1.model.OrderItem;
import Nhom1.Demo_Nhom1.model.PendingPayment;
import Nhom1.Demo_Nhom1.service.CartService;
import Nhom1.Demo_Nhom1.service.MoMoPaymentService;
import Nhom1.Demo_Nhom1.service.OrderService;
import Nhom1.Demo_Nhom1.service.PendingPaymentService;
import Nhom1.Demo_Nhom1.service.VNPayPaymentService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/payment")
public class PaymentController {
    
    @Autowired
    private MoMoPaymentService moMoPaymentService;
    
    @Autowired
    private VNPayPaymentService vnPayPaymentService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private PendingPaymentService pendingPaymentService;
    
    @Autowired
    private CartService cartService;
    
    /**
     * Tạo payment URL cho MoMo
     * Tạo pending payment trước, chỉ tạo order sau khi thanh toán thành công
     */
    @PostMapping("/momo/create")
    public ResponseEntity<?> createMoMoPayment(@RequestBody Map<String, Object> request, 
                                                Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Vui lòng đăng nhập để thanh toán");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            String userId = authentication.getName();
            String username = authentication.getName();
            String shippingAddress = (String) request.get("shippingAddress");
            String phone = (String) request.get("phone");
            String note = (String) request.get("note");
            
            @SuppressWarnings("unchecked")
            List<String> selectedBookIds = (List<String>) request.get("selectedBookIds");
            
            // Tạo pending payment (chỉ các items được chọn)
            PendingPayment pendingPayment = pendingPaymentService.createPendingPayment(
                userId, username, shippingAddress, phone, "MOMO", note, selectedBookIds
            );
            
            String payUrl = moMoPaymentService.createPayment(
                pendingPayment.getId(), 
                pendingPayment.getTotalAmount().longValue(),
                "Thanh toan don hang " + pendingPayment.getId()
            );
            
            Map<String, String> response = new HashMap<>();
            response.put("payUrl", payUrl);
            response.put("pendingPaymentId", pendingPayment.getId());
            response.put("message", "Success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * IPN (Instant Payment Notification) từ MoMo
     */
    @PostMapping("/momo/notify")
    public ResponseEntity<?> moMoNotify(@RequestBody Map<String, String> params) {
        try {
            if (!moMoPaymentService.verifySignature(params)) {
                Map<String, Object> response = new HashMap<>();
                response.put("resultCode", 97);
                response.put("message", "Invalid signature");
                return ResponseEntity.ok(response);
            }
            
            String pendingPaymentId = params.get("orderId");
            Integer resultCode = Integer.parseInt(params.get("resultCode"));
            
            if (resultCode == 0) {
                Optional<PendingPayment> optional = pendingPaymentService.findPendingById(pendingPaymentId);
                
                if (optional.isPresent()) {
                    PendingPayment pendingPayment = optional.get();
                    
                    Order order = new Order();
                    order.setUserId(pendingPayment.getUserId());
                    order.setUsername(pendingPayment.getUsername());
                    order.setShippingAddress(pendingPayment.getShippingAddress());
                    order.setPhone(pendingPayment.getPhone());
                    order.setPaymentMethod("MOMO");
                    order.setNote(pendingPayment.getNote());
                    order.setStatus("CONFIRMED");
                    order.setPaymentStatus("PAID");
                    order.setTotalAmount(pendingPayment.getTotalAmount());
                    
                    order.setItems(pendingPayment.getItems().stream()
                        .map(cartItem -> {
                            OrderItem orderItem = new OrderItem();
                            orderItem.setBookId(cartItem.getBookId());
                            orderItem.setBookTitle(cartItem.getBookTitle());
                            orderItem.setImageUrl(cartItem.getImageUrl());
                            orderItem.setQuantity(cartItem.getQuantity());
                            orderItem.setPrice(cartItem.getPrice());
                            orderItem.setSubtotal(cartItem.getPrice() * cartItem.getQuantity());
                            return orderItem;
                        })
                        .collect(Collectors.toList())
                    );
                    
                    orderService.save(order);
                    cartService.clearCartWithoutRestoreStock(pendingPayment.getUserId());
                    pendingPaymentService.updateStatus(pendingPaymentId, "COMPLETED");
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("resultCode", 0);
            response.put("message", "Success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("resultCode", 99);
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * API Tạo payment URL cho VNPay
     * Quy trình: Tạo bản ghi thanh toán chờ (PendingPayment) -> Lấy URL thanh toán từ VNPay -> Trả về cho Frontend redirect
     */
    @PostMapping("/vnpay/create")
    public ResponseEntity<?> createVNPayPayment(@RequestBody Map<String, Object> request, 
                                                 HttpServletRequest httpRequest,
                                                 Authentication authentication) {
        try {
            // Kiểm tra đăng nhập
            if (authentication == null || !authentication.isAuthenticated()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Vui lòng đăng nhập để thanh toán");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            String userId = authentication.getName();
            String username = authentication.getName();
            String shippingAddress = (String) request.get("shippingAddress");
            String phone = (String) request.get("phone");
            String note = (String) request.get("note");
            String ipAddress = getClientIP(httpRequest); // Lấy IP người dùng để gửi sang VNPay
            
            @SuppressWarnings("unchecked")
            List<String> selectedBookIds = (List<String>) request.get("selectedBookIds");
            
            // 1. Lưu thông tin thanh toán vào DB với trạng thái PENDING
            // Chỉ bao gồm các sản phẩm mà người dùng đã tick chọn trong giỏ hàng
            PendingPayment pendingPayment = pendingPaymentService.createPendingPayment(
                userId, username, shippingAddress, phone, "VNPAY", note, selectedBookIds
            );
            
            // 2. Gọi service để build URL sang cổng thanh toán VNPay
            String payUrl = vnPayPaymentService.createPayment(
                pendingPayment.getId(), // ID này sẽ là vnp_TxnRef
                pendingPayment.getTotalAmount().longValue(),
                "Thanh toan don hang " + pendingPayment.getId(),
                ipAddress
            );
            
            // 3. Trả về payUrl để trình duyệt chuyển hướng người dùng
            Map<String, String> response = new HashMap<>();
            response.put("payUrl", payUrl);
            response.put("pendingPaymentId", pendingPayment.getId());
            response.put("message", "Success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
