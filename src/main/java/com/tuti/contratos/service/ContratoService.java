package com.tuti.contratos.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tuti.common.exception.RecursoNoEncontradoException;
import com.tuti.contratos.dto.ContratoResponse;
import com.tuti.contratos.repository.ContratoRepository;

@Service
public class ContratoService {
    private final ContratoRepository repository;

    public ContratoService(ContratoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ContratoResponse consultar(Long id) {
        return repository.findById(id).map(ContratoResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el contrato " + id + "."));
    }
}

