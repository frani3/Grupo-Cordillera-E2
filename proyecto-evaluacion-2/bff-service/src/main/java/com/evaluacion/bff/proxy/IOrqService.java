package com.evaluacion.bff.proxy;

import com.evaluacion.bff.model.DataResponse;

public interface IOrqService {
    DataResponse fetchData(String requestId, String authToken);
}
