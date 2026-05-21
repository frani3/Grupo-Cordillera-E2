package com.evaluacion.bff.proxy;

import com.evaluacion.bff.model.DataResponse;
import java.util.List;
import java.util.Map;

public interface IOrqService {
    DataResponse fetchData(String requestId, String authToken);
    List<Map<String, Object>> fetchVentas(String authToken);
}
