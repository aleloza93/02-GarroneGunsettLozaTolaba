package com.tuti.daos.service;

import com.tuti.daos.dto.ContratoResponse;
import com.tuti.daos.entities.Contrato;

public interface ContratoService {
    ContratoResponse consultar(Long id);
    
    Contrato buscarPorId(Long id);
}