package com.example.demo.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "bids")
public class Bid {
    @Id
    private String id;
    
    private String taskId;
    private String taskTitle;
    
    private String bidderId;
    private String bidderName;
    private String bidderEmail;
    
    private double bidAmount;
    private String message;
    private String estimatedTime; // e.g., "2 days"
    
    private String status; // PENDING, ACCEPTED, REJECTED
    
    private String createdAt;
}