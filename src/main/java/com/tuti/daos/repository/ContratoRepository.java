package com.tuti.daos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tuti.daos.entities.Contrato;
import com.tuti.daos.entities.Factura;

import jakarta.persistence.LockModeType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT c FROM Contrato c WHERE c.id = :id")
	Optional<Contrato> findByIdParaFacturar(@Param("id") Long id);

}