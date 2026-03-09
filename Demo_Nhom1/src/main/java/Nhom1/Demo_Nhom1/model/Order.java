package Nhom1.Demo_Nhom1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {
    @Id
    private String id;
    
    private String userId;
    private String username;
    private List<OrderItem> items = new ArrayList<>();
    private Double totalAmount;
    private String status; // PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    private String paymentMethod; // COD, MOMO, VNPAY
    private String paymentStatus = "UNPAID"; // UNPAID, PAID, REFUNDED
    private String shippingAddress;
    private String phone;
    private String note;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private String viewToken; // one-time token to allow public viewing after external payment redirects
}
