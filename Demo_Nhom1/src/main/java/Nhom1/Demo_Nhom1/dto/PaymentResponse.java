package Nhom1.Demo_Nhom1.dto;

import lombok.Data;

@Data
public class PaymentResponse {
    private String payUrl;
    private String message;
    private Integer resultCode;
}
