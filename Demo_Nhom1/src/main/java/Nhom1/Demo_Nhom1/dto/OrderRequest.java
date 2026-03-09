package Nhom1.Demo_Nhom1.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private String shippingAddress;
    private String phone;
    private String paymentMethod;
    private String note;
    private List<String> selectedBookIds; // IDs sản phẩm được chọn (null = tất cả)
}
