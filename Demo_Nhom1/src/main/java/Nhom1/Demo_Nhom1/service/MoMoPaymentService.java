package Nhom1.Demo_Nhom1.service;

import com.google.gson.Gson;

import Nhom1.Demo_Nhom1.config.MoMoConfig;

import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MoMoPaymentService {
    
    @Autowired
    private MoMoConfig moMoConfig;
    
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    
    public String createPayment(String orderId, long amount, String orderInfo) throws Exception {
        String requestId = UUID.randomUUID().toString();
        
        // Create raw signature
        String rawSignature = "accessKey=" + moMoConfig.getAccessKey() +
                "&amount=" + amount +
                "&extraData=" +
                "&ipnUrl=" + moMoConfig.getNotifyUrl() +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + moMoConfig.getPartnerCode() +
                "&redirectUrl=" + moMoConfig.getReturnUrl() +
                "&requestId=" + requestId +
                "&requestType=captureWallet";
        
        // Generate signature
        String signature = hmacSHA256(rawSignature, moMoConfig.getSecretKey());
        
        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("partnerCode", moMoConfig.getPartnerCode());
        requestBody.put("accessKey", moMoConfig.getAccessKey());
        requestBody.put("requestId", requestId);
        requestBody.put("amount", amount);
        requestBody.put("orderId", orderId);
        requestBody.put("orderInfo", orderInfo);
        requestBody.put("redirectUrl", moMoConfig.getReturnUrl());
        requestBody.put("ipnUrl", moMoConfig.getNotifyUrl());
        requestBody.put("requestType", "captureWallet");
        requestBody.put("extraData", "");
        requestBody.put("lang", "vi");
        requestBody.put("signature", signature);
        
        // Send request to MoMo
        String jsonBody = gson.toJson(requestBody);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(moMoConfig.getEndpoint()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Parse response
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = gson.fromJson(response.body(), Map.class);
        
        if (responseMap.get("resultCode") != null && 
            ((Double) responseMap.get("resultCode")).intValue() == 0) {
            return (String) responseMap.get("payUrl");
        } else {
            throw new Exception("MoMo payment creation failed: " + responseMap.get("message"));
        }
    }
    
    public boolean verifySignature(Map<String, String> params) throws Exception {
        String signature = params.get("signature");
        
        String rawSignature = "accessKey=" + moMoConfig.getAccessKey() +
                "&amount=" + params.get("amount") +
                "&extraData=" + params.getOrDefault("extraData", "") +
                "&message=" + params.get("message") +
                "&orderId=" + params.get("orderId") +
                "&orderInfo=" + params.get("orderInfo") +
                "&orderType=" + params.get("orderType") +
                "&partnerCode=" + params.get("partnerCode") +
                "&payType=" + params.get("payType") +
                "&requestId=" + params.get("requestId") +
                "&responseTime=" + params.get("responseTime") +
                "&resultCode=" + params.get("resultCode") +
                "&transId=" + params.get("transId");
        
        String calculatedSignature = hmacSHA256(rawSignature, moMoConfig.getSecretKey());
        
        return signature.equals(calculatedSignature);
    }
    
    private String hmacSHA256(String data, String key) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes()));
    }
}
