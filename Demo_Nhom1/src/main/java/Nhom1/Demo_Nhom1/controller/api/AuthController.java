package Nhom1.Demo_Nhom1.controller.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import Nhom1.Demo_Nhom1.dto.AuthResponse;
import Nhom1.Demo_Nhom1.dto.LoginRequest;
import Nhom1.Demo_Nhom1.dto.RegisterRequest;
import Nhom1.Demo_Nhom1.model.User;
import Nhom1.Demo_Nhom1.security.JwtTokenProvider;
import Nhom1.Demo_Nhom1.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        
        String token = tokenProvider.generateToken(authentication);
        User user = userService.findByUsername(loginRequest.getUsername());
        
        return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getUsername(), user.getRoles()));
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(registerRequest.getPassword());
        user.setFullName(registerRequest.getFullName());
        user.setPhone(registerRequest.getPhone());
        user.setAddress(registerRequest.getAddress());
        
        User savedUser = userService.registerUser(user);
        
        if (savedUser == null) {
            return ResponseEntity.badRequest().body("Username or email already exists");
        }
        
        return ResponseEntity.ok("User registered successfully");
    }
}
