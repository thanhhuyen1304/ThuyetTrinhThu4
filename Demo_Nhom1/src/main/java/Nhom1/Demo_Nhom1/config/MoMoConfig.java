package Nhom1.Demo_Nhom1.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

@Configuration
@Getter
public class MoMoConfig {
    
    @Value("${momo.partner.code}")
    private String partnerCode;
    
    @Value("${momo.access.key}")
    private String accessKey;
    
    @Value("${momo.secret.key}")
    private String secretKey;
    
    @Value("${momo.endpoint}")
    private String endpoint;
    
    @Value("${momo.return.url}")
    private String returnUrl;
    
    @Value("${momo.notify.url}")
    private String notifyUrl;
}
