package com.tuti.contratos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.tuti.contratos.model.Contrato;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {}

