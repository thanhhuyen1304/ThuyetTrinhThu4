package Nhom1.Demo_Nhom1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String username;
    
    @Indexed(unique = true)
    private String email;
    
    private String password;
    private String fullName;
    private String phone;
    private String address;
    private String avatarUrl;
    private Set<String> roles = new HashSet<>();
    private boolean enabled = true;
    
    // OAuth2 fields
    private String oauthProvider; // GOOGLE, FACEBOOK, etc.
    private String oauthId; // ID từ OAuth provider
    
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
