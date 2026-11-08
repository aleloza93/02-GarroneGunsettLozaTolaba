package com.tuti.daos.service;

import java.util.List;

import com.tuti.daos.dto.PagoResponse;
import com.tuti.daos.dto.RegistrarPagoRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface PagoService {
    PagoResponse registrar(@NotNull @Valid RegistrarPagoRequest request);

    PagoResponse consultar(Long id);

    List<PagoResponse> listar(Long facturaId);
}