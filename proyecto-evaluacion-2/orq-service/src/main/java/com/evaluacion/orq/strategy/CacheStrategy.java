package com.evaluacion.orq.strategy;

import org.springframework.stereotype.Component;

@Component
public class CacheStrategy implements ProcessingStrategy {

    @Override
    public String process(String requestId, Object rawData) {
        return String.format("CACHE[%s]: datos servidos desde caché local", requestId);
    }

    @Override
    public String getName() {
        return "cache";
    }
}
