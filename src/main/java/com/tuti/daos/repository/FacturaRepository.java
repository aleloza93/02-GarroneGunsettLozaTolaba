package com.tuti.daos.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuti.daos.entities.Factura;
import com.tuti.daos.utilidades.EstadoFactura;

import jakarta.persistence.LockModeType;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {

    boolean existsByContratoIdAndPeriodoAndMensualTrueAndEstadoNot(
            Long contratoId, String periodo, EstadoFactura estado);

    @Query("""
        SELECT f FROM Factura f
        WHERE (:contratoId IS NULL OR f.contrato.id = :contratoId)
          AND (:periodo IS NULL OR f.periodo = :periodo)
          AND (:estado IS NULL OR f.estado = :estado)
          AND (:vencidas IS NULL
               OR (:vencidas = TRUE  AND f.fechaVencimiento <  :hoy)
               OR (:vencidas = FALSE AND f.fechaVencimiento >= :hoy))
        ORDER BY f.id DESC
        """)
    List<Factura> buscar(@Param("contratoId") Long contratoId,
                         @Param("periodo") String periodo,
                         @Param("estado") EstadoFactura estado,
                         @Param("vencidas") Boolean vencidas,
                         @Param("hoy") LocalDate hoy);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Factura f WHERE f.id = :id")
    Optional<Factura> buscarParaActualizar(@Param("id") Long id);
}


/*
public interface FacturaRepository extends JpaRepository<Factura, Long> {

    // Listar todas las facturas ordenadas por ID.
    List<Factura> findAllByOrderByIdAsc();

    // Buscar facturas de un contrato.
    List<Factura> findByContrato_IdOrderByIdAsc(Long contratoId);

    // Buscar facturas de un período.
    List<Factura> findByPeriodoOrderByIdAsc(YearMonth periodo);

    // Buscar facturas por estado.
    List<Factura> findByEstadoOrderByIdAsc(EstadoFactura estado);

    // Verificar si ya existe una factura mensual no anulada
    // para un contrato y período determinados.
    boolean existsByContrato_IdAndPeriodoAndFacturacionMensualTrueAndEstadoNot(
            Long contratoId,
            YearMonth periodo,
            EstadoFactura estado
    );

    // Recuperar una factura bloqueando su registro en la base de datos.
    // Se utilizará durante el registro de un pago.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Factura f WHERE f.id = :id")
    Optional<Factura> findByIdParaPago(@Param("id") Long id);
}*/