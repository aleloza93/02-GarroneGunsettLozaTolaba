package com.tuti.daos.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuti.daos.common.exception.RecursoNoEncontradoException;
import com.tuti.daos.dto.ContratoResponse;
import com.tuti.daos.repository.ContratoRepository;
import com.tuti.daos.service.ContratoService;

@Service
public class ContratoServiceImpl implements ContratoService {
    private final ContratoRepository repository;

    public ContratoServiceImpl(ContratoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public ContratoResponse consultar(Long id) {
        return repository.findById(id).map(ContratoResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el contrato " + id + "."));
    }
}