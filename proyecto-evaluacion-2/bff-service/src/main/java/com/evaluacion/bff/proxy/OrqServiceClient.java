package com.evaluacion.bff.proxy;

import com.evaluacion.bff.model.DataResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    @Override
    public List<Map<String, Object>> fetchVentas(String authToken) {
        try {
            var response = restTemplate.exchange(
                    serviceUrl + "/api/ventas",
                    HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
