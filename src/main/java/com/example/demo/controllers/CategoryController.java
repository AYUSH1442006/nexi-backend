package com.example.demo.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    // Get all task categories
    @GetMapping
    public ResponseEntity<?> getCategories() {
        List<Map<String, String>> categories = List.of(
            Map.of("id", "cleaning", "name", "Cleaning", "icon", "🧹"),
            Map.of("id", "moving", "name", "Moving & Delivery", "icon", "🚚"),
            Map.of("id", "handyman", "name", "Handyman", "icon", "🔧"),
            Map.of("id", "gardening", "name", "Gardening", "icon", "🌱"),
            Map.of("id", "painting", "name", "Painting", "icon", "🎨"),
            Map.of("id", "plumbing", "name", "Plumbing", "icon", "🚰"),
            Map.of("id", "electrical", "name", "Electrical", "icon", "⚡"),
            Map.of("id", "tech", "name", "Tech Support", "icon", "💻"),
            Map.of("id", "tutoring", "name", "Tutoring", "icon", "📚"),
            Map.of("id", "photography", "name", "Photography", "icon", "📷"),
            Map.of("id", "writing", "name", "Writing & Content", "icon", "✍️"),
            Map.of("id", "design", "name", "Design", "icon", "🎭"),
            Map.of("id", "pet", "name", "Pet Care", "icon", "🐕"),
            Map.of("id", "event", "name", "Event Planning", "icon", "🎉"),
            Map.of("id", "other", "name", "Other", "icon", "📦")
        );

        return ResponseEntity.ok(categories);
    }
}




