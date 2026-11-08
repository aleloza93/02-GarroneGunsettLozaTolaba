package com.tuti.daos.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tuti.daos.entities.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    boolean existsByFactura_Id(Long facturaId);
    Optional<Pago> findByFactura_Id(Long facturaId);
    List<Pago> findAllByOrderByIdAsc();
}
