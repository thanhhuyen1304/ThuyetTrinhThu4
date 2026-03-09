package Nhom1.Demo_Nhom1.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private String orderId;
    private Long amount;
    private String orderInfo;
    private String returnUrl;
    private String notifyUrl;
}
