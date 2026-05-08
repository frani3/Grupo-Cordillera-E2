package com.evaluacion.bff.proxy;

import com.evaluacion.bff.model.DataResponse;

// PATRÓN PROXY: Justificación técnica para evaluación parcial 2
// Subject: interfaz común para ServiceProxy y OrqServiceClient.
// BffController depende de esta abstracción → principio DIP.
public interface IOrqService {
    DataResponse fetchData(String requestId, String authToken);
}
