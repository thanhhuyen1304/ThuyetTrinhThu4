package Nhom1.Demo_Nhom1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private Long bookId;
    private String bookTitle;
    private Integer quantity;
    private Double price;
    private String imageUrl;
}
