package com.tuti.daos.incidentes.repository;

import com.tuti.daos.incidentes.domain.Incidente;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IncidenteRepository extends JpaRepository<Incidente, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select incidente from Incidente incidente where incidente.id = :id")
    Optional<Incidente> findByIdForUpdate(@Param("id") Long id);
}
