package com.servicio1.demo.repository;

import com.servicio1.demo.model.PosTransaction;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// PATRON SINGLETON: Holder Pattern para la Base de Datos simulada
@Repository
public class PosTransactionRepository {

    protected PosTransactionRepository() {} // Constructor protegido para que Spring pueda instanciar via CGLIB

    private static class DatabaseHolder {
        // CopyOnWriteArrayList es Thread-Safe. Funciona como nuestra base de datos simulada.
        static final List<PosTransaction> INSTANCE = new CopyOnWriteArrayList<>();
    }

    public static List<PosTransaction> getDatabase() {
        return DatabaseHolder.INSTANCE;
    }

    public PosTransaction save(PosTransaction transaction) {
        if (transaction.getId() == null) {
            transaction.setId((long) (getDatabase().size() + 1));
        }
        getDatabase().add(transaction);
        return transaction;
    }

    public List<PosTransaction> findAll() {
        return new ArrayList<>(getDatabase());
    }
}
