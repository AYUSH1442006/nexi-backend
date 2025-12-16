package com.example.demo.controllers;

import com.example.demo.models.Task;
import com.example.demo.models.User;
import com.example.demo.repositories.TaskRepository;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    @Autowired
    private TaskRepository taskRepo;

    @Autowired
    private UserRepository userRepo;

    // Create a new task
    @PostMapping("/create")
    public ResponseEntity<?> createTask(@RequestBody Task task) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepo.findByEmail(email);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }
            
            task.setPosterId(user.getId());
            task.setPosterName(user.getName());
            task.setPosterEmail(user.getEmail());
            task.setStatus("OPEN");
            task.setCreatedAt(LocalDateTime.now().toString());
            task.setUpdatedAt(LocalDateTime.now().toString());
            
            Task savedTask = taskRepo.save(task);
            
            // Update user's posted tasks count
            user.setTasksPosted(user.getTasksPosted() + 1);
            userRepo.save(user);
            
            return ResponseEntity.ok(savedTask);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Get all open tasks
    @GetMapping("/open")
    public ResponseEntity<?> getOpenTasks() {
        try {
            List<Task> tasks = taskRepo.findByStatus("OPEN");
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Get tasks by category
    @GetMapping("/category/{category}")
    public ResponseEntity<?> getTasksByCategory(@PathVariable String category) {
        try {
            List<Task> tasks = taskRepo.findByCategory(category);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Search tasks
    @GetMapping("/search")
    public ResponseEntity<?> searchTasks(@RequestParam String keyword) {
        try {
            List<Task> tasks = taskRepo.searchByTitle(keyword);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Get task by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getTaskById(@PathVariable String id) {
        try {
            Optional<Task> task = taskRepo.findById(id);
            if (task.isPresent()) {
                return ResponseEntity.ok(task.get());
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Task not found"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Get my posted tasks
    @GetMapping("/my-tasks")
    public ResponseEntity<?> getMyTasks() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepo.findByEmail(email);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }
            
            List<Task> tasks = taskRepo.findByPosterId(user.getId());
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Get tasks assigned to me
    @GetMapping("/assigned-to-me")
    public ResponseEntity<?> getAssignedTasks() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepo.findByEmail(email);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }
            
            List<Task> tasks = taskRepo.findByAssignedTo(user.getId());
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Update task
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable String id, @RequestBody Task updatedTask) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepo.findByEmail(email);
            
            Optional<Task> taskOpt = taskRepo.findById(id);
            if (!taskOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Task not found"));
            }
            
            Task task = taskOpt.get();
            
            // Only task poster can update
            if (!task.getPosterId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only update your own tasks"));
            }
            
            task.setTitle(updatedTask.getTitle());
            task.setDescription(updatedTask.getDescription());
            task.setCategory(updatedTask.getCategory());
            task.setLocation(updatedTask.getLocation());
            task.setBudget(updatedTask.getBudget());
            task.setDeadline(updatedTask.getDeadline());
            task.setUpdatedAt(LocalDateTime.now().toString());
            
            Task saved = taskRepo.save(task);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Delete task
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable String id) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepo.findByEmail(email);
            
            Optional<Task> taskOpt = taskRepo.findById(id);
            if (!taskOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Task not found"));
            }
            
            Task task = taskOpt.get();
            
            // Only task poster can delete
            if (!task.getPosterId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only delete your own tasks"));
            }
            
            taskRepo.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Task deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Mark task as in progress
    @PutMapping("/{id}/start")
    public ResponseEntity<?> startTask(@PathVariable String id) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepo.findByEmail(email);
            
            Optional<Task> taskOpt = taskRepo.findById(id);
            if (!taskOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Task not found"));
            }
            
            Task task = taskOpt.get();
            
            // Only assigned user can start task
            if (!task.getAssignedTo().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only the assigned user can start this task"));
            }
            
            task.setStatus("IN_PROGRESS");
            task.setUpdatedAt(LocalDateTime.now().toString());
            Task saved = taskRepo.save(task);
            
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Mark task as completed
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeTask(@PathVariable String id) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepo.findByEmail(email);
            
            Optional<Task> taskOpt = taskRepo.findById(id);
            if (!taskOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Task not found"));
            }
            
            Task task = taskOpt.get();
            
            // Only assigned user can complete task
            if (!task.getAssignedTo().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only the assigned user can complete this task"));
            }
            
            task.setStatus("COMPLETED");
            task.setUpdatedAt(LocalDateTime.now().toString());
            Task saved = taskRepo.save(task);
            
            // Update both users' completed tasks count
            user.setTasksCompleted(user.getTasksCompleted() + 1);
            userRepo.save(user);
            
            Optional<User> posterOpt = userRepo.findById(task.getPosterId());
            if (posterOpt.isPresent()) {
                User poster = posterOpt.get();
                poster.setTasksCompleted(poster.getTasksCompleted() + 1);
                userRepo.save(poster);
            }
            
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
}