package com.example.demo.controllers;

import com.example.demo.models.User;
import com.example.demo.models.Wallet;
import com.example.demo.models.WalletTransaction;
import com.example.demo.repositories.UserRepository;
import com.example.demo.repositories.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/wallet")
//@CrossOrigin(origins = "*")
public class WalletController {

    @Autowired
    private WalletRepository walletRepo;

    @Autowired
    private UserRepository userRepo;

    // Get wallet details
    @GetMapping
    public ResponseEntity<?> getWallet() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepo.findByEmail(email);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }

            Optional<Wallet> walletOpt = walletRepo.findByUserId(user.getId());
            
            if (!walletOpt.isPresent()) {
                // Create wallet if it doesn't exist
                Wallet newWallet = new Wallet();
                newWallet.setUserId(user.getId());
                newWallet.setBalance(0.0);
                Wallet saved = walletRepo.save(newWallet);
                return ResponseEntity.ok(saved);
            }

            return ResponseEntity.ok(walletOpt.get());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Add money to wallet
    @PostMapping("/add-money")
    public ResponseEntity<?> addMoney(@RequestBody Map<String, Double> request) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepo.findByEmail(email);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }

            Double amount = request.get("amount");
            if (amount == null || amount <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid amount"));
            }

            // Get or create wallet
            Wallet wallet = walletRepo.findByUserId(user.getId())
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet();
                    newWallet.setUserId(user.getId());
                    newWallet.setBalance(0.0);
                    return newWallet;
                });

            // Add money
            wallet.setBalance(wallet.getBalance() + amount);

            // Create transaction record
            WalletTransaction tx = new WalletTransaction();
            tx.setType("CREDIT");
            tx.setAmount(amount);
            tx.setReference("Money added to wallet");
            tx.setCreatedAt(LocalDateTime.now().toString());

            wallet.getTransactions().add(tx);
            walletRepo.save(wallet);

            return ResponseEntity.ok(Map.of(
                "message", "Money added successfully",
                "balance", wallet.getBalance()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Get transaction history
    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepo.findByEmail(email);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }

            Optional<Wallet> walletOpt = walletRepo.findByUserId(user.getId());
            
            if (!walletOpt.isPresent()) {
                return ResponseEntity.ok(Map.of("transactions", new Object[]{}));
            }

            return ResponseEntity.ok(Map.of("transactions", walletOpt.get().getTransactions()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
}