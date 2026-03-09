package Nhom1.Demo_Nhom1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pending_payments")
public class PendingPayment {
    @Id
    private String id; // Dùng làm orderId cho payment gateway
    
    private String userId;
    private String username;
    private List<CartItem> items;
    private Double totalAmount;
    private String paymentMethod; // MOMO, VNPAY
    private String shippingAddress;
    private String phone;
    private String note;
    private String status; // PENDING, COMPLETED, EXPIRED, CANCELLED
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime expiresAt; // Hết hạn sau 15 phút
}
