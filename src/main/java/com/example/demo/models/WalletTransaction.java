package com.example.demo.models;

import lombok.Data;

@Data
public class WalletTransaction {
    private String type; // ADD, DEBIT, CREDIT
    private double amount;
    private String reference;
    private String createdAt;
}
