package com.tuti.daos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tuti.daos.entities.Incidente;

import jakarta.persistence.LockModeType;

public interface IncidenteResolucionRepository extends JpaRepository<Incidente, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Incidente i WHERE i.id = :id")
    Optional<Incidente> findByIdParaBloqueo(@Param("id") Long id);
}
