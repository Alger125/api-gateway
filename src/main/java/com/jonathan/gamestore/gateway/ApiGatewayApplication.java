package com.jonathan.gamestore.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * PUNTO DE ENTRADA PRINCIPAL: API Gateway
 *
 * Centraliza las peticiones externas al ecosistema GameStore en el puerto 8080.
 * Resuelve dinamicamente las instancias de catalog-service y sales-service
 * a traves de Eureka Server con balanceo de carga integrado (LoadBalancer).
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}