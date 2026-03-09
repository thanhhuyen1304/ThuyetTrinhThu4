package Nhom1.Demo_Nhom1.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import Nhom1.Demo_Nhom1.model.Order;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private SpringTemplateEngine templateEngine;
    
    @Value("${spring.mail.username:noreply@bookstore.com}")
    private String fromEmail;
    
    @Async
    public void sendOrderConfirmationEmail(Order order, String toEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Xác Nhận Đơn Hàng #" + order.getId().substring(0, 8).toUpperCase());
            
            Context context = new Context();
            context.setVariable("order", order);
            context.setVariable("orderIdShort", order.getId().substring(0, 8).toUpperCase());
            context.setVariable("formattedDate", order.getCreatedAt().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            context.setVariable("formattedTotal", formatCurrency(order.getTotalAmount()));
            
            String htmlContent = templateEngine.process("email/order-confirmation", context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            // Log error but don't fail the order creation
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
    
    @Async
    public void sendOrderStatusUpdateEmail(Order order, String toEmail, String oldStatus, String newStatus) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Cập Nhật Đơn Hàng #" + order.getId().substring(0, 8).toUpperCase());
            
            Context context = new Context();
            context.setVariable("order", order);
            context.setVariable("orderIdShort", order.getId().substring(0, 8).toUpperCase());
            context.setVariable("oldStatus", getStatusText(oldStatus));
            context.setVariable("newStatus", getStatusText(newStatus));
            context.setVariable("formattedTotal", formatCurrency(order.getTotalAmount()));
            
            String htmlContent = templateEngine.process("email/order-status-update", context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
    
    @Async
    public void sendWelcomeEmail(String toEmail, String username) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Chào Mừng Đến Với BookStore!");
            
            Context context = new Context();
            context.setVariable("username", username);
            
            String htmlContent = templateEngine.process("email/welcome", context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
    
    private String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getInstance(Locale.of("vi", "VN"));
        return formatter.format(amount) + " VND";
    }
    
    private String getStatusText(String status) {
        return switch (status) {
            case "PENDING" -> "Chờ xử lý";
            case "CONFIRMED" -> "Đã xác nhận";
            case "SHIPPED" -> "Đang giao";
            case "DELIVERED" -> "Đã giao";
            case "CANCELLED" -> "Đã hủy";
            default -> status;
        };
    }
}
