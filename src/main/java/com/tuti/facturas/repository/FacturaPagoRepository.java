package com.tuti.facturas.repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tuti.facturas.model.Factura;

/** Lo usamos para avanzar con Pagos; despues lo unificamos con el repositorio de Javier. */
public interface FacturaPagoRepository extends JpaRepository<Factura, Long> {
    // El bloqueo dura hasta terminar la transaccion, no solo mientras leemos la factura.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Factura f where f.id = :id")
    Optional<Factura> buscarParaActualizar(@Param("id") Long id);
}
