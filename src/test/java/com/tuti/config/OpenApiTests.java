package com.tuti.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiTests {
    @Autowired MockMvc mvc;

    @Test
    void especificacionDescribeOperacionesYErroresReales() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("HogarYA - TP02 DAOS"))
                .andExpect(jsonPath("$.paths['/pagos'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/pagos'].post.responses['201'].headers.Location").exists())
                .andExpect(jsonPath("$.paths['/pagos'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/pagos'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/pagos'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/pagos/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/contratos/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/facturas/{id}']").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.RegistrarPagoRequest.properties.interes.description").exists())
                .andExpect(jsonPath("$.paths['/pagos'].get.parameters[0].name").value("facturaId"))
                .andExpect(jsonPath("$.paths['/pagos'].get.parameters[0].required").value(false));
    }

    @Test
    void swaggerSirveLaInterfazYConfiguracion() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Swagger UI")));
        mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/v3/api-docs"));
    }
}
