package com.ufersa.amqpservice;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ufersa.amqpservice.services.AMQPService;

@RestController
@RequestMapping("/amqpservice")
public class AMQPController {

    @Autowired
    private AMQPService amqpService;

    @GetMapping("/broker")
    public Map<String, String> obterBrokerRabbitMQ() {
        return Map.of("host", "localhost", "port", "5672");
    }

    @GetMapping("/historico")
    public List<String> obterHistorico() {
        return amqpService.lerHistorico();
    }
}


