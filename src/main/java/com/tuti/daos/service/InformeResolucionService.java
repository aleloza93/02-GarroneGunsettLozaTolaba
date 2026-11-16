package com.tuti.daos.service;

import java.util.List;

import com.tuti.daos.dto.InformeResponse;
import com.tuti.daos.dto.RegistrarInformeRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface InformeResolucionService {
    InformeResponse registrar(@NotNull @Valid RegistrarInformeRequest request);

    InformeResponse consultar(Long id);

    List<InformeResponse> listar(Long incidenteId, String responsable);
}
