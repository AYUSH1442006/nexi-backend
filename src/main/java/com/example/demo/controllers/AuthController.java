package com.example.demo.controllers;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.demo.models.User;
import com.example.demo.repositories.UserRepository;
import com.example.demo.security.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JWTUtil jwtUtil;

   
    // Update user profile during registration with timestamp
@PostMapping("/register")
public ResponseEntity<?> register(@RequestBody User user) {
    System.out.println("=== REGISTER ENDPOINT HIT ===");
    System.out.println("Email: " + user.getEmail());
    
    try {
        if (userRepo.findByEmail(user.getEmail()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "User already exists"));
        }
        
        user.setPassword(encoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now().toString()); // Add timestamp
        User savedUser = userRepo.save(user);
        
        // Generate token immediately after registration
        String token = jwtUtil.generateToken(savedUser.getEmail());
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully");
        response.put("token", token);
        response.put("user", Map.of(
            "id", savedUser.getId(),
            "name", savedUser.getName(),
            "email", savedUser.getEmail()
        ));
        
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", e.getMessage()));
    }
}

// Verify token endpoint
@GetMapping("/verify")
public ResponseEntity<?> verifyToken() {
    try {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByEmail(email);
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid token"));
        }
        
        user.setPassword(null);
        return ResponseEntity.ok(Map.of(
            "valid", true,
            "user", user
        ));
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("valid", false, "error", "Invalid token"));
    }
}

// Forgot password - Generate reset token (simplified version)
@PostMapping("/forgot-password")
public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> data) {
    try {
        String email = data.get("email");
        User user = userRepo.findByEmail(email);
        
        if (user == null) {
            // Don't reveal if email exists
            return ResponseEntity.ok(Map.of("message", "If email exists, reset link will be sent"));
        }
        
        // In production, send email with reset token
        // For now, just return success
        return ResponseEntity.ok(Map.of("message", "If email exists, reset link will be sent"));
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", e.getMessage()));
    }
}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        System.out.println("Login endpoint hit with email: " + user.getEmail());
        
        try {
            User dbUser = userRepo.findByEmail(user.getEmail());

            if (dbUser != null && encoder.matches(user.getPassword(), dbUser.getPassword())) {
                String token = jwtUtil.generateToken(dbUser.getEmail());
                
                Map<String, String> response = new HashMap<>();
                response.put("token", token);
                response.put("email", dbUser.getEmail());
                
                return ResponseEntity.ok(response);
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid credentials"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
}