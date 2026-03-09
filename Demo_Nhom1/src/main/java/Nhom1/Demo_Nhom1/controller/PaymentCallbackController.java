package Nhom1.Demo_Nhom1.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import Nhom1.Demo_Nhom1.model.Order;
import Nhom1.Demo_Nhom1.model.OrderItem;
import Nhom1.Demo_Nhom1.model.PendingPayment;
import Nhom1.Demo_Nhom1.service.CartService;
import Nhom1.Demo_Nhom1.service.MoMoPaymentService;
import Nhom1.Demo_Nhom1.service.OrderService;
import Nhom1.Demo_Nhom1.service.PendingPaymentService;
import Nhom1.Demo_Nhom1.service.VNPayPaymentService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/payment")
public class PaymentCallbackController {
    
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
     * Callback từ MoMo sau khi thanh toán
     * Tạo order từ pending payment nếu thanh toán thành công
     */
    @GetMapping("/momo/callback")
    public String moMoCallback(@RequestParam Map<String, String> params) {
        System.out.println("=== MoMo Callback (PaymentCallbackController) ===");
        System.out.println("Params: " + params);
        
        try {
            // Verify signature
            if (!moMoPaymentService.verifySignature(params)) {
                return "redirect:/orders?payment=failed&message=Invalid+signature";
            }
            
            String pendingPaymentId = params.get("orderId");
            Integer resultCode = Integer.parseInt(params.get("resultCode"));
            
            if (resultCode == 0) {
                // Payment success - Tạo order từ pending payment
                Optional<PendingPayment> optional = pendingPaymentService.findPendingById(pendingPaymentId);
                
                if (optional.isPresent()) {
                    PendingPayment pendingPayment = optional.get();
                    
                    // Tạo order
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
                    
                    // Convert CartItem to OrderItem
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
                    
                    // generate a one-time view token so external redirect can view order
                    String token = java.util.UUID.randomUUID().toString();
                    order.setViewToken(token);
                    order = orderService.save(order);
                    
                    // Xóa giỏ hàng KHÔNG cộng lại stock (vì đã chuyển sang order)
                    cartService.clearCartWithoutRestoreStock(pendingPayment.getUserId());
                    
                    // Cập nhật pending payment status
                    pendingPaymentService.updateStatus(pendingPaymentId, "COMPLETED");
                    
                    // Redirect về trang chi tiết đơn hàng (include token so user can view without login)
                    return "redirect:/orders/detail?id=" + order.getId() + "&token=" + token + "&payment=success";
                } else {
                    return "redirect:/orders?payment=failed&message=Pending+payment+not+found";
                }
            } else {
                // Payment failed
                pendingPaymentService.updateStatus(pendingPaymentId, "CANCELLED");
                return "redirect:/orders?payment=failed&message=" + params.get("message");
            }
        } catch (Exception e) {
            return "redirect:/orders?payment=error&message=" + e.getMessage();
        }
    }
    
    /**
     * Callback từ VNPay sau khi thanh toán
     * Đây là nơi VNPay redirect người dùng về sau khi thực hiện thao tác trên cổng thanh toán
     */
    @GetMapping("/vnpay/callback")
    public String vnPayCallback(@RequestParam Map<String, String> params, Model model) {
        try {
            System.out.println("=== VNPay Callback ===");
            System.out.println("Params: " + params);
            
            // 1. Kiểm tra chữ ký (Security Check)
            // Đảm bảo dữ liệu gửi về là từ VNPay, không bị giả mạo
            if (!vnPayPaymentService.verifySignature(new HashMap<>(params))) {
                System.out.println("Invalid signature!");
                model.addAttribute("status", "failed");
                model.addAttribute("message", "Chữ ký không hợp lệ");
                model.addAttribute("redirectUrl", "/orders?payment=failed&message=Invalid+signature");
                return "payment-processing";
            }
            
            String pendingPaymentId = params.get("vnp_TxnRef"); // Mã đơn hàng mình gửi sang
            String responseCode = params.get("vnp_ResponseCode"); // Mã phản hồi kết quả giao dịch
            
            System.out.println("Pending Payment ID: " + pendingPaymentId);
            System.out.println("Response Code: " + responseCode);
            
            // 2. Xử lý kết quả giao dịch
            // Mã "00" có nghĩa là thanh toán thành công
            if ("00".equals(responseCode)) {
                // Lấy thông tin thanh toán chờ từ DB
                Optional<PendingPayment> optional = pendingPaymentService.findPendingById(pendingPaymentId);
                
                if (optional.isPresent()) {
                    PendingPayment pendingPayment = optional.get();
                    
                    System.out.println("Found pending payment for user: " + pendingPayment.getUserId());
                    
                    // 3. Chính thức tạo Order (Đơn hàng) sau khi đã thu tiền thành công
                    Order order = new Order();
                    order.setUserId(pendingPayment.getUserId());
                    order.setUsername(pendingPayment.getUsername());
                    order.setShippingAddress(pendingPayment.getShippingAddress());
                    order.setPhone(pendingPayment.getPhone());
                    order.setPaymentMethod("VNPAY");
                    order.setNote(pendingPayment.getNote());
                    order.setStatus("CONFIRMED");
                    order.setPaymentStatus("PAID");
                    order.setTotalAmount(pendingPayment.getTotalAmount());
                    
                    // Chuyển danh sách sản phẩm từ PendingPayment sang Order
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
                    
                    // Tạo view token để người dùng xem lại đơn hàng ngay lập tức mà không cần session phức tạp
                    String token = java.util.UUID.randomUUID().toString();
                    order.setViewToken(token);
                    order = orderService.save(order);
                    System.out.println("Order created: " + order.getId());
                    
                    // 4. Dọn dẹp giỏ hàng và cập nhật trạng thái thanh toán chờ
                    cartService.clearCartWithoutRestoreStock(pendingPayment.getUserId());
                    pendingPaymentService.updateStatus(pendingPaymentId, "COMPLETED");
                    
                    System.out.println("Payment completed successfully!");
                    System.out.println("======================");
                    
                    // Trả về view trung gian để hiển thị thông báo thành công
                    model.addAttribute("status", "success");
                    model.addAttribute("message", "Thanh toán thành công!");
                    model.addAttribute("redirectUrl", "/orders/detail?id=" + order.getId() + "&token=" + token + "&payment=success");
                    return "payment-processing";
                } else {
                    System.out.println("Pending payment not found!");
                    model.addAttribute("status", "failed");
                    model.addAttribute("message", "Không tìm thấy thông tin thanh toán");
                    model.addAttribute("redirectUrl", "/orders?payment=failed&message=Pending+payment+not+found");
                    return "payment-processing";
                }
            } else {
                // Giao dịch không thành công (Người dùng hủy, lỗi thẻ, ...)
                System.out.println("Payment failed with code: " + responseCode);
                pendingPaymentService.updateStatus(pendingPaymentId, "CANCELLED");
                
                model.addAttribute("status", "failed");
                model.addAttribute("message", "Thanh toán thất bại (Mã lỗi: " + responseCode + ")");
                model.addAttribute("redirectUrl", "/orders?payment=failed&code=" + responseCode);
                return "payment-processing";
            }
        } catch (Exception e) {
            System.out.println("Exception in VNPay callback: " + e.getMessage());
            model.addAttribute("status", "failed");
            model.addAttribute("message", "Có lỗi xảy ra: " + e.getMessage());
            model.addAttribute("redirectUrl", "/orders?payment=error&message=" + e.getMessage());
            return "payment-processing";
        }
    }
    
    /**
     * Trang test VNPay callback (chỉ dùng cho development)
     */
    @GetMapping("/vnpay/test")
    public String vnPayTest(@RequestParam(required = false) String pendingId, Model model) {
        if (pendingId != null) {
            model.addAttribute("pendingId", pendingId);
        }
        return "vnpay-test";
    }
}
