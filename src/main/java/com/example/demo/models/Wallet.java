package com.example.demo.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "wallets")
public class Wallet {

    @Id
    private String id;

    private String userId;
    private double balance = 0.0;

    private List<WalletTransaction> transactions = new ArrayList<>();
}
