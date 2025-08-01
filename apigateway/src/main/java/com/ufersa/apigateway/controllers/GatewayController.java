package com.ufersa.apigateway.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
public class GatewayController {

    private final DiscoveryClient discoveryClient;

    @GetMapping("/mqtt")
    public ResponseEntity<?> localizarServicoMQTT() {
        List<ServiceInstance> instances = discoveryClient.getInstances("servico-mqtt");
        if (instances.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Serviço MQTT indisponível.");
        }
        ServiceInstance instancia = instances.get(0);
        Map<String, String> info = new HashMap<>();
        info.put("host", instancia.getHost());
        info.put("port", String.valueOf(instancia.getPort()));
        return ResponseEntity.ok(info);
    }

    @GetMapping("/rabbitmq")
    public ResponseEntity<?> localizarServicoRabbitMQ() {
        List<ServiceInstance> instances = discoveryClient.getInstances("servico-rabbitmq");
        if (instances.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Serviço RabbitMQ indisponível.");
        }
        ServiceInstance instancia = instances.get(0);
        Map<String, String> info = new HashMap<>();
        info.put("host", instancia.getHost());
        info.put("port", String.valueOf(instancia.getPort()));
        return ResponseEntity.ok(info);
    }
}

