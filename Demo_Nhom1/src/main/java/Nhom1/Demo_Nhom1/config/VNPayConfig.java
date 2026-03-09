package Nhom1.Demo_Nhom1.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

@Configuration
@Getter
public class VNPayConfig {
    
    @Value("${vnpay.tmn.code}")
    private String tmnCode;
    
    @Value("${vnpay.hash.secret}")
    private String hashSecret;
    
    @Value("${vnpay.endpoint}")
    private String endpoint;
    
    @Value("${vnpay.return.url}")
    private String returnUrl;
}
