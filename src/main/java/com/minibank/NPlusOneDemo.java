package com.minibank;

import java.util.List;

public class NPlusOneDemo {
    public static void main(String[] args) {
        BankService bankService = new BankService();

        /*
        System.out.println("=== 1. ПРОБЛЕМА N+1 (FetchType.EAGER) ===");
        List<Client> clients = bankService.getAllClientsWithAccounts();
        for (Client client : clients) {
            System.out.println("Клиент: " + client.getName() + ", счетов: " + client.getAccounts().size());
        }*/

        System.out.println("\n=== 2. ИСПРАВЛЕНО (JOIN FETCH) ===");
        List<Client> fixedClients = bankService.getAllClientsWithAccountsFixed();
        for (Client client : fixedClients) {
            System.out.println("Клиент: " + client.getName() + ", счетов: " + client.getAccounts().size());
        }

        HibernateUtil.shutdown();
    }
}
