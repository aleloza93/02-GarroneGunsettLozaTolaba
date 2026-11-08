package com.tuti.pagos.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tuti.pagos.model.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    boolean existsByFactura_Id(Long facturaId);
    Optional<Pago> findByFactura_Id(Long facturaId);
    List<Pago> findAllByOrderByIdAsc();
}
