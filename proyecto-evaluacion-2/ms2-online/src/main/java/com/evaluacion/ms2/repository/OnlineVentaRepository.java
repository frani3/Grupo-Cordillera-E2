package com.evaluacion.ms2.repository;

import com.evaluacion.ms2.model.OnlineVenta;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

// PATRON SINGLETON: Holder Pattern — misma estrategia que el MS1
@Repository
public class OnlineVentaRepository {

    protected OnlineVentaRepository() {}

    private static class DatabaseHolder {
        static final List<OnlineVenta> INSTANCE = new CopyOnWriteArrayList<>();
    }

    public static List<OnlineVenta> getDatabase() {
        return DatabaseHolder.INSTANCE;
    }

    public OnlineVenta save(OnlineVenta venta) {
        if (venta.getId() == null) {
            venta.setId((long) (getDatabase().size() + 1));
        }
        getDatabase().add(venta);
        return venta;
    }

    public List<OnlineVenta> findAll() {
        return new ArrayList<>(getDatabase());
    }

    public List<OnlineVenta> findUltimosDias(int dias) {
        LocalDate desde = LocalDate.now().minusDays(dias);
        return getDatabase().stream()
                .filter(v -> v.getFecha() != null && !v.getFecha().isBefore(desde))
                .collect(Collectors.toList());
    }
}
