package com.example.demo.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    
    private String name;
    private String email;
    private String password;
    private String phone;
    private GeoLocation location;

    private String bio;
    private String profileImage;
    
    private String role;  // ← ADD THIS LINE: "POSTER", "TASKER", or "BOTH"
    
    private double rating = 0.0;
    private int totalReviews = 0;
    private int tasksCompleted = 0;
    private int tasksPosted = 0;
    
    private List<String> skills = new ArrayList<>();
    private boolean isVerified = false;
    
    private String createdAt;
}