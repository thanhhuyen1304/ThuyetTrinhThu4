package Nhom1.Demo_Nhom1.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Nhom1.Demo_Nhom1.model.Cart;
import Nhom1.Demo_Nhom1.model.CartItem;
import Nhom1.Demo_Nhom1.model.PendingPayment;
import Nhom1.Demo_Nhom1.repository.PendingPaymentRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PendingPaymentService {
    
    @Autowired
    private PendingPaymentRepository pendingPaymentRepository;
    
    @Autowired
    private CartService cartService;
    
    /**
     * Tạo pending payment từ giỏ hàng (tất cả items)
     */
    public PendingPayment createPendingPayment(String userId, String username, 
                                                String shippingAddress, String phone, 
                                                String paymentMethod, String note) {
        return createPendingPayment(userId, username, shippingAddress, phone, paymentMethod, note, null);
    }
    
    /**
     * Tạo pending payment từ giỏ hàng (chỉ các items được chọn)
     */
    public PendingPayment createPendingPayment(String userId, String username, 
                                                String shippingAddress, String phone, 
                                                String paymentMethod, String note,
                                                List<String> selectedBookIds) {
        Cart cart = cartService.getCartByUserId(userId);
        
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống");
        }
        
        // Lọc items nếu có selectedBookIds
        List<CartItem> items;
        if (selectedBookIds != null && !selectedBookIds.isEmpty()) {
            items = cart.getItems().stream()
                .filter(item -> selectedBookIds.contains(String.valueOf(item.getBookId())))
                .collect(java.util.stream.Collectors.toList());
            if (items.isEmpty()) {
                throw new RuntimeException("Không có sản phẩm nào được chọn");
            }
        } else {
            items = new ArrayList<>(cart.getItems());
        }
        
        // Tính tổng tiền từ các items đã lọc
        double totalAmount = items.stream()
            .mapToDouble(item -> item.getPrice() * item.getQuantity())
            .sum();
        
        PendingPayment pendingPayment = new PendingPayment();
        pendingPayment.setId(UUID.randomUUID().toString().replace("-", ""));
        pendingPayment.setUserId(userId);
        pendingPayment.setUsername(username);
        
        pendingPayment.setItems(items);
        pendingPayment.setTotalAmount(totalAmount);
        
        pendingPayment.setPaymentMethod(paymentMethod);
        pendingPayment.setShippingAddress(shippingAddress);
        pendingPayment.setPhone(phone);
        pendingPayment.setNote(note);
        pendingPayment.setStatus("PENDING");
        
        // Hết hạn sau 15 phút
        pendingPayment.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        
        return pendingPaymentRepository.save(pendingPayment);
    }
    
    /**
     * Tìm pending payment theo ID
     */
    public Optional<PendingPayment> findById(String id) {
        return pendingPaymentRepository.findById(id);
    }
    
    /**
     * Tìm pending payment đang chờ
     */
    public Optional<PendingPayment> findPendingById(String id) {
        return pendingPaymentRepository.findByIdAndStatus(id, "PENDING");
    }
    
    /**
     * Cập nhật status
     */
    public PendingPayment updateStatus(String id, String status) {
        Optional<PendingPayment> optional = pendingPaymentRepository.findById(id);
        if (optional.isPresent()) {
            PendingPayment pendingPayment = optional.get();
            pendingPayment.setStatus(status);
            return pendingPaymentRepository.save(pendingPayment);
        }
        return null;
    }
    
    /**
     * Xóa pending payment sau khi hoàn thành
     */
    public void delete(String id) {
        pendingPaymentRepository.deleteById(id);
    }
    
    /**
     * Xóa các pending payment đã hết hạn
     */
    public void cleanupExpiredPayments() {
        List<PendingPayment> expired = pendingPaymentRepository
                .findByExpiresAtBeforeAndStatus(LocalDateTime.now(), "PENDING");
        
        for (PendingPayment payment : expired) {
            payment.setStatus("EXPIRED");
            pendingPaymentRepository.save(payment);
        }
    }
}
