package com.example.demo.repositories;

import com.example.demo.models.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {
    List<Task> findByStatus(String status);
    List<Task> findByPosterId(String posterId);
    List<Task> findByAssignedTo(String assignedTo);
    List<Task> findByCategory(String category);
    
    @Query("{'title': {$regex: ?0, $options: 'i'}}")
    List<Task> searchByTitle(String keyword);
}