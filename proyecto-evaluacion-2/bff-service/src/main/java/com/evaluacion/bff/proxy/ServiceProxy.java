package com.evaluacion.bff.proxy;

import com.evaluacion.bff.model.DataResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

// PATRÓN PROXY: Justificación técnica para evaluación parcial 2
//
// ServiceProxy implementa tres tipos de Proxy simultáneamente:
//
//   1. Proxy de Protección → valida el token Bearer antes de propagar.
//      Centraliza la seguridad: ningún servicio interno necesita duplicarla.
//
//   2. Proxy de Auditoría  → registra request/response con timestamp
//      para trazabilidad y cumplimiento (OWASP Logging Cheat Sheet).
//
//   3. Proxy de Error      → captura excepciones del RealSubject y las
//      convierte en respuestas tipadas. No expone stack traces al cliente.
//
// El cliente (BffController) llama al Proxy igual que al servicio real
// gracias a IOrqService → transparencia total (principio LSP).
@Service
public class ServiceProxy implements IOrqService {

    private final IOrqService realSubject;

    // Constructor usado por Spring — ORQ_SERVICE_URL se inyecta desde application.properties
    public ServiceProxy(
            RestTemplate restTemplate,
            @Value("${orq.service.url}") String serviceUrl) {
        this.realSubject = new OrqServiceClient(restTemplate, serviceUrl);
    }

    // Constructor package-private para tests unitarios (inyecta mock del RealSubject)
    ServiceProxy(IOrqService realSubject) {
        this.realSubject = realSubject;
    }

    @Override
    public DataResponse fetchData(String requestId, String authToken) {
        validateBearerToken(authToken);
        auditLog("REQUEST", requestId, authToken);

        DataResponse response;
        try {
            response = realSubject.fetchData(requestId, authToken);
        } catch (Exception e) {
            auditLog("ERROR", requestId, e.getMessage());
            return DataResponse.error("Error interno del proxy: " + e.getMessage());
        }

        auditLog("RESPONSE", requestId, response.status());
        return response;
    }

    private void validateBearerToken(String authToken) {
        if (authToken == null || authToken.isBlank()) {
            throw new SecurityException("Token de autorización ausente. Incluir 'Authorization: Bearer <token>'");
        }
        if (!authToken.startsWith("Bearer ")) {
            throw new SecurityException("Formato de token inválido. Se requiere esquema Bearer.");
        }
    }

    private void auditLog(String phase, String requestId, String detail) {
        System.out.printf("[AUDIT][%s] phase=%s requestId=%s detail=%s%n",
                Instant.now(), phase, requestId, truncate(detail, 80));
    }

    private String truncate(String value, int max) {
        if (value == null) return "null";
        return value.length() > max ? value.substring(0, max) + "..." : value;
    }
}
