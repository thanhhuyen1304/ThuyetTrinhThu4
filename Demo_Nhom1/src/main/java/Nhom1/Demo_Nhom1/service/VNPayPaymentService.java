package Nhom1.Demo_Nhom1.service;

import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Nhom1.Demo_Nhom1.config.VNPayConfig;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VNPayPaymentService {
    
    @Autowired
    private VNPayConfig vnPayConfig;
    
    /**
     * Hàm quan trọng: Tạo URL thanh toán để redirect sang VNPay
     */
    public String createPayment(String orderId, long amount, String orderInfo, String ipAddress) throws Exception {
        // 1. Khởi tạo các tham số cơ bản theo tài liệu VNPay 2.1.0
        Map<String, String> vnp_Params = new TreeMap<>(); // TreeMap giúp tự động sắp xếp key theo alphabet (yêu cầu của VNPay)
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode()); // Mã website (lấy từ application.properties)
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100)); // VNPay tính theo đơn vị VND x 100
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", orderId); // Mã giao dịch của hệ thống mình (UUID)
        vnp_Params.put("vnp_OrderInfo", orderInfo); // Nội dung thanh toán
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl()); // URL để VNPay quay về sau khi xong
        
        // 2. Xử lý IP Address (VNPay yêu cầu IPv4, tránh lỗi nếu chạy localhost IPv6)
        if (ipAddress == null || ipAddress.equals("0:0:0:0:0:0:0:1")) {
            vnp_Params.put("vnp_IpAddr", "127.0.0.1");
        } else {
            vnp_Params.put("vnp_IpAddr", ipAddress);
        }
        
        // 3. Thiết lập thời gian (Dùng múi giờ Việt Nam)
        TimeZone tz = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        sdf.setTimeZone(tz);
        
        String vnp_CreateDate = sdf.format(new Date());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate); // Ngày tạo giao dịch
        
        Calendar calendar = Calendar.getInstance(tz);
        calendar.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = sdf.format(calendar.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate); // Thời gian hết hạn giao dịch (15 phút)
        
        // 4. Xây dựng chuỗi dữ liệu băm (Hash Data) và Query String
        // QUAN TRỌNG: VNPay 2.1.0 yêu cầu hash trên chuỗi đã được URL Encode
        // Và khoảng trắng phải được thay bằng %20 (thay vì dấu + mặc định của Java)
        StringJoiner sj = new StringJoiner("&");
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        
        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                sj.add(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()) + "=" + 
                       URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()).replace("+", "%20"));
            }
        }
        String hashData = sj.toString(); // Đây là chuỗi sẽ dùng để băm chữ ký
        
        // 5. Tạo chữ ký SecureHash bằng thuật toán HMAC-SHA512
        // Chữ ký trả ra PHẢI LÀ CHỮ HOA (UPPERCASE)
        String vnp_SecureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData).toUpperCase();
        
        // 6. Tạo URL cuối cùng bao gồm cả chữ ký
        String paymentUrl = vnPayConfig.getEndpoint() + "?" + hashData + "&vnp_SecureHash=" + vnp_SecureHash;
        
        // Ghi log để kiểm tra nếu cần
        System.out.println("=== VNPay DEBUG ===");
        System.out.println("Raw Data to Hash: " + hashData);
        System.out.println("Secure Hash result: " + vnp_SecureHash);
        
        return paymentUrl;
    }
    
    /**
     * Hàm kiểm tra chữ ký do VNPay gửi về (Callback)
     */
    public boolean verifySignature(Map<String, String> params) throws Exception {
        // Lấy chữ ký từ VNPay gửi về và xóa nó ra khỏi map để tính toán chữ ký mới đối soát
        String vnp_SecureHash = params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");
        
        if (vnp_SecureHash == null) return false;
        
        // Sắp xếp các tham số còn lại
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        
        // Xây dựng lại chuỗi hash data từ các tham số VNPay gửi về
        StringJoiner sj = new StringJoiner("&");
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                sj.add(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()) + "=" + 
                       URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()).replace("+", "%20"));
            }
        }
        
        // Tính toán lại mã băm trên hệ thống của mình
        String calculatedHash = hmacSHA512(vnPayConfig.getHashSecret(), sj.toString()).toUpperCase();
        
        // So sánh mã băm của mình và VNPay gửi về
        return calculatedHash.equalsIgnoreCase(vnp_SecureHash);
    }
    
    /**
     * Thuật toán băm HMAC-SHA512
     */
    private String hmacSHA512(String key, String data) throws Exception {
        Mac hmac512 = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmac512.init(secretKey);
        byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Hex.encodeHexString(result); // Trả về dạng hex chuỗi thường, sau đó dùng toUpperCase() bên ngoài
    }
}

