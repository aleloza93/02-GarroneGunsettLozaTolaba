package com.tuti.daos.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

// Esto arma la portada de Swagger; no cambia las reglas de nuestros servicios.
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "HogarYA - TP02 DAOS",
        version = "1.0",
        description = """
                API del equipo Garrone, Gunsett, Loza y Tolaba.
                Por ahora estan disponibles Pagos y la consulta auxiliar de Contratos.
                Try it out ejecuta operaciones reales: un POST valido guarda el pago.
                No se admiten pagos parciales ni mas de un pago por factura.
                El destino del enlace a Facturas se completara al integrar S01.
                """))
public class OpenApiConfig {}
