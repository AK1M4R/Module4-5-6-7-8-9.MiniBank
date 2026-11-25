package com.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BankService {

    public Client registerClient(String name, String email) {
        EntityManager em = HibernateUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            // Создаем нового клиента (id сгенерируется автоматически)
            Client client = new Client(name, email);
            em.persist(client);
            tx.commit();
            return client;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Error registering client", e);
        } finally {
            em.close();
        }
    }

    public Account openAccount(Long clientId, String accountNumber) {
        EntityManager em = HibernateUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Client client = em.find(Client.class, clientId);
            if (client == null) {
                throw new IllegalArgumentException("Client not found: " + clientId);
            }
            
            Account account = new Account(client, accountNumber, BigDecimal.ZERO);
            em.persist(account);
            tx.commit();
            return account;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Error opening account", e);
        } finally {
            em.close();
        }
    }

    public Account getAccount(Long accountId) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            Account account = em.find(Account.class, accountId);
            if (account == null) {
                throw new IllegalArgumentException("Account not found: " + accountId);
            }
            return account;
        } finally {
            em.close();
        }
    }

    public Client getClient(Long clientId) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            Client client = em.find(Client.class, clientId);
            if (client == null) {
                throw new IllegalArgumentException("Client not found: " + clientId);
            }
            return client;
        } finally {
            em.close();
        }
    }
    
    public void deposit(Long accountId, BigDecimal amount) {
        EntityManager em = HibernateUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Account account = em.find(Account.class, accountId);
            if (account == null) throw new IllegalArgumentException("Account not found");

            account.setBalance(account.getBalance().add(amount));
            
            Transaction transaction = new Transaction(account, amount, "DEPOSIT", LocalDateTime.now());
            em.persist(transaction);
            
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Error depositing", e);
        } finally {
            em.close();
        }
    }

    // ВНИМАНИЕ: Баг с исчезновением денег сохранен логически!
    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        EntityManager em = HibernateUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Account from = em.find(Account.class, fromAccountId);
            if (from == null) throw new IllegalArgumentException("Account not found: " + fromAccountId);

            // Проверка достаточности средств
            if (from.getBalance().compareTo(amount) < 0) {
                throw new IllegalStateException("Insufficient funds on account " + fromAccountId);
            }

            // 1. Списываем деньги
            from.setBalance(from.getBalance().subtract(amount));
            Transaction outTx = new Transaction(from, amount.negate(), "TRANSFER_OUT", LocalDateTime.now());
            em.persist(outTx);

            // 2. Проверяем получателя (выбросит исключение, если не найден)
            Account to = em.find(Account.class, toAccountId);
            if (to == null) throw new IllegalArgumentException("Account not found: " + toAccountId);

            // 3. Начисляем деньги
            to.setBalance(to.getBalance().add(amount));
            Transaction inTx = new Transaction(to, amount, "TRANSFER_IN", LocalDateTime.now());
            em.persist(inTx);

            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback(); // В случае ошибки откатываем всё (Hibernate ACID)
            throw e;
        } finally {
            em.close();
        }
    }

    // === МЕТОД ДЛЯ ДЕМОНСТРАЦИИ ПРОБЛЕМЫ N+1 ===
    public List<Client> getAllClientsWithAccounts() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            // В этот момент из-за FetchType.EAGER Hibernate сделает кучу запросов
            return em.createQuery("SELECT c FROM Client c", Client.class).getResultList();
        } finally {
            em.close();
        }
    }
    
    // === ИСПРАВЛЕНИЕ N+1 ===
    public List<Client> getAllClientsWithAccountsFixed() {
        // Используем EntityManager
        var em = HibernateUtil.getEntityManager();
        try {
            // LEFT JOIN FETCH заставляет БД сделать JOIN и вернуть клиентов со счетами ОДНИМ запросом
            String hql = "SELECT DISTINCT c FROM Client c LEFT JOIN FETCH c.accounts";
            return em.createQuery(hql, Client.class).getResultList();
        } finally {
            em.close();
        }
    }
}