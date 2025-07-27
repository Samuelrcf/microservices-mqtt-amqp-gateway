package com.ufersa.mqttservice.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mqttservice")
public class MQTTController {

    @GetMapping("/broker")
    public Map<String, String> getBrokerAddress() {
        Map<String, String> response = new HashMap<>();
        response.put("brokerUrl", "tcp://localhost:1885");
        return response;
    }
}

