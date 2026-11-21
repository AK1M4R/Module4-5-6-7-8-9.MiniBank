package com.minibank;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BankService {

    public Client registerClient(String name, String email) {
        String sql = "INSERT INTO clients (name, email) VALUES (?, ?) RETURNING id";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Client(rs.getLong("id"), name, email);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error registering client", e);
        }
        return null;
    }

    public Account openAccount(Long clientId, String accountNumber) {
        String sql = "INSERT INTO accounts (client_id, account_number, balance) VALUES (?, ?, 0) RETURNING id, balance";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, clientId);
            pstmt.setString(2, accountNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Account(rs.getLong("id"), clientId, accountNumber, rs.getBigDecimal("balance"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error opening account", e);
        }
        return null;
    }

    public Account getAccount(Long accountId) {
        String sql = "SELECT id, client_id, account_number, balance FROM accounts WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, accountId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Account(
                            rs.getLong("id"),
                            rs.getLong("client_id"),
                            rs.getString("account_number"),
                            rs.getBigDecimal("balance")
                    );
                } else {
                    throw new IllegalArgumentException("Account not found: " + accountId);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching account", e);
        }
    }

    public Client getClient(Long clientId) {
        String sql = "SELECT id, name, email FROM clients WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, clientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Client(rs.getLong("id"), rs.getString("name"), rs.getString("email"));
                } else {
                    throw new IllegalArgumentException("Client not found: " + clientId);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching client", e);
        }
    }
    
    public void deposit(Long accountId, BigDecimal amount) {
        Account account = getAccount(accountId);
        updateBalance(accountId, account.getBalance().add(amount));
        recordTransaction(accountId, amount, "DEPOSIT");
    }

    // ВНИМАНИЕ: Баг
    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        Account from = getAccount(fromAccountId);
        
        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds on account " + fromAccountId);
        }

        // 1. Списываем деньги
        updateBalance(fromAccountId, from.getBalance().subtract(amount));
        recordTransaction(fromAccountId, amount.negate(), "TRANSFER_OUT");

        // 2. Проверяем получателя
        Account to = getAccount(toAccountId); 
        
        // 3. Начисляем деньги
        updateBalance(toAccountId, to.getBalance().add(amount));
        recordTransaction(toAccountId, amount, "TRANSFER_IN");
    }

    private void updateBalance(Long accountId, BigDecimal newBalance) {
        String sql = "UPDATE accounts SET balance = ? WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, newBalance);
            pstmt.setLong(2, accountId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating balance", e);
        }
    }

    private void recordTransaction(Long accountId, BigDecimal amount, String type) {
        String sql = "INSERT INTO transactions (account_id, amount, type, transaction_date) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, accountId);
            pstmt.setBigDecimal(2, amount);
            pstmt.setString(3, type);
            pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error recording transaction", e);
        }
    }
}
