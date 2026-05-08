package com.evaluacion.bff.controller;

import com.evaluacion.bff.model.DataResponse;
import com.evaluacion.bff.proxy.ServiceProxy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Tests de capa web con MockMvc. @WebMvcTest carga solo Controller + filtros.
// @MockBean ServiceProxy evita levantar el contexto completo de Spring.
@WebMvcTest(BffController.class)
@DisplayName("BffController — Endpoints REST")
class BffControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private ServiceProxy serviceProxy;

    @Test
    @DisplayName("GET /api/proxy/data → 200 con payload cuando token es válido")
    void getData_returns200_withValidToken() throws Exception {
        DataResponse stub = DataResponse.ok("registro-123", "orq-service");
        when(serviceProxy.fetchData(eq("default"), anyString())).thenReturn(stub);

        mockMvc.perform(get("/api/proxy/data")
                        .header("Authorization", "Bearer valid-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")))
                .andExpect(jsonPath("$.data",   is("registro-123")))
                .andExpect(jsonPath("$.source", is("orq-service")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/proxy/data?id pasa el requestId al proxy")
    void getData_passesRequestId_toProxy() throws Exception {
        when(serviceProxy.fetchData(eq("user-42"), anyString()))
                .thenReturn(DataResponse.ok("resultado", "orq-service"));

        mockMvc.perform(get("/api/proxy/data")
                        .param("id", "user-42")
                        .header("Authorization", "Bearer tok"))
                .andExpect(status().isOk());

        verify(serviceProxy).fetchData("user-42", "Bearer tok");
    }

    @Test
    @DisplayName("GET /api/proxy/data → 401 cuando el proxy rechaza el token")
    void getData_returns401_whenProxyThrowsSecurityException() throws Exception {
        when(serviceProxy.fetchData(anyString(), isNull()))
                .thenThrow(new SecurityException("Token ausente"));

        mockMvc.perform(get("/api/proxy/data").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is("error")));
    }

    @Test
    @DisplayName("GET /api/proxy/health → 200 sin llamar al proxy")
    void health_returns200_withoutCallingProxy() throws Exception {
        mockMvc.perform(get("/api/proxy/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")))
                .andExpect(jsonPath("$.source", is("bff-service")));

        verifyNoInteractions(serviceProxy);
    }
}
