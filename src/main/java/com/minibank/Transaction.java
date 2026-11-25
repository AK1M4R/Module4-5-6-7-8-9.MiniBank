package com.minibank;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER) 
    @JoinColumn(name = "account_id", nullable = false) 
    private Account account;

    // В БД колонка называется NUMERIC(19,4)
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String type;

    // В БД колонка называется transaction_date
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime timestamp;

    // ОБЯЗАТЕЛЬНО: пустой конструктор для Hibernate
    public Transaction() {
    }

    public Transaction(Account account, BigDecimal amount, String type, LocalDateTime timestamp) {
        this.account = account;
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
    }

    // Геттеры
    public Long getId() { return id; }
    
    public Account getAccount() { return account; }
    
    public Long getAccountId() { return account != null ? account.getId() : null; }

    public BigDecimal getAmount() { return amount; }
    public String getType() { return type; }
    public LocalDateTime getTimestamp() { return timestamp; }
}