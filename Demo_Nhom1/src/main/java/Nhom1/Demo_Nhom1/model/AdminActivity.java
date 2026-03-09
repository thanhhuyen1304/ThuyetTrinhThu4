package Nhom1.Demo_Nhom1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "admin_activities")
public class AdminActivity {
    @Id
    private String id;
    
    private String adminUsername;
    private String adminId;
    private String action; // CREATE, UPDATE, DELETE, VIEW, LOGIN, LOGOUT
    private String entityType; // BOOK, USER, ORDER, CATEGORY
    private String entityId;
    private String entityName;
    private String description;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime timestamp = LocalDateTime.now();
    
    // Additional details for better tracking
    private String oldValue; // Giá trị cũ (cho UPDATE)
    private String newValue; // Giá trị mới (cho UPDATE/CREATE)
    private java.util.Map<String, Object> metadata; // Thông tin bổ sung
    
    // Constructor for quick logging
    public AdminActivity(String adminUsername, String action, String entityType, String entityId, String description) {
        this.adminUsername = adminUsername;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }
}
