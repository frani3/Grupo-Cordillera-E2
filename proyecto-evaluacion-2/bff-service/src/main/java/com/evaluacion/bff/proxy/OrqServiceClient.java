package com.evaluacion.bff.proxy;

import com.evaluacion.bff.model.DataResponse;
import org.springframework.web.client.RestTemplate;

// PATRÓN PROXY: Justificación técnica para evaluación parcial 2
// RealSubject: lógica de negocio real (HTTP al orq-service).
// No conoce autenticación ni auditoría → principio SRP.
public class OrqServiceClient implements IOrqService {

    private final RestTemplate restTemplate;
    private final String serviceUrl;

    public OrqServiceClient(RestTemplate restTemplate, String serviceUrl) {
        this.restTemplate = restTemplate;
        this.serviceUrl = serviceUrl;
    }

    @Override
    public DataResponse fetchData(String requestId, String authToken) {
        String endpoint = serviceUrl + "/api/data?id=" + requestId;
        try {
            DataResponse response = restTemplate.getForObject(endpoint, DataResponse.class);
            return response != null ? response : DataResponse.error("Respuesta vacía de orq-service");
        } catch (Exception e) {
            return DataResponse.error("orq-service no disponible: " + e.getMessage());
        }
    }
}
