package com.evaluacion.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

// PATRÓN PROXY: Justificación técnica para evaluación parcial 2
// Punto de entrada Spring Boot. Define el bean RestTemplate que
// ServiceProxy usa para delegar llamadas al orq-service interno.
@SpringBootApplication
public class BffApplication {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public static void main(String[] args) {
        SpringApplication.run(BffApplication.class, args);
    }
}
