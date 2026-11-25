package com.minibank;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class HibernateUtil {
    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("minibankPU");

    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    // Метод для корректного закрытия фабрики (нужно вызывать в конце программы)
    public static void shutdown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
