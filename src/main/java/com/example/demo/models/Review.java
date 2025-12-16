package com.example.demo.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "reviews")
public class Review {
    @Id
    private String id;
    
    private String taskId;
    private String reviewerId; // Who gave the review
    private String reviewerName;
    
    private String reviewedUserId; // Who received the review
    private String reviewedUserName;
    
    private int rating; // 1-5
    private String comment;
    
    private String createdAt;
}