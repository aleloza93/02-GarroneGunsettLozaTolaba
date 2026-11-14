package com.tuti.daos.informes.repository;

import com.tuti.daos.informes.domain.InformeResolucion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InformeResolucionRepository extends JpaRepository<InformeResolucion, Long> {

    List<InformeResolucion> findAllByOrderByFechaHoraResolucionDesc();

    List<InformeResolucion> findAllByIncidente_IdOrderByFechaHoraResolucionDesc(Long incidenteId);
}
