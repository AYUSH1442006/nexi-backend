package com.example.demo.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "tasks")
public class Task {

    @Id
    private String id;

    private String title;
    private String description;
    private String category;

    private GeoLocation location; // ✅ MAP LOCATION

    private double budget;
    private String deadline;

    private String posterId;
    private String posterName;
    private String posterEmail;

    private String assignedTo;
    private String assignedToName;

    private String status;

    private List<String> images = new ArrayList<>();
    private List<String> requiredSkills = new ArrayList<>();

    private String createdAt;
    private String updatedAt;

    private int bidCount = 0;
}
