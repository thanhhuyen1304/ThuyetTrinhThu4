package Nhom1.Demo_Nhom1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private Long bookId;
    private String bookTitle;
    private String imageUrl;
    private Integer quantity;
    private Double price;
    private Double subtotal;
}
