package Nhom1.Demo_Nhom1.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import Nhom1.Demo_Nhom1.dto.RegisterRequest;
import Nhom1.Demo_Nhom1.model.User;
import Nhom1.Demo_Nhom1.service.UserService;

@Controller
public class AuthWebController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
    
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }
    
    @PostMapping("/register")
    public String register(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            Model model) {
        
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setAddress(address);
        
        User savedUser = userService.registerUser(user);
        
        if (savedUser == null) {
            RegisterRequest req = new RegisterRequest();
            req.setUsername(username);
            req.setEmail(email);
            req.setFullName(fullName);
            req.setPhone(phone);
            req.setAddress(address);
            model.addAttribute("error", "Username hoặc email đã tồn tại");
            model.addAttribute("registerRequest", req);
            return "register";
        }
        
        model.addAttribute("success", "Registration successful! Please login.");
        return "login";
    }
}
