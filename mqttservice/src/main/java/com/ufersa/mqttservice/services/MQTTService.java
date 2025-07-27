package com.ufersa.mqttservice.services;

import java.util.Arrays;
import java.util.List;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class MQTTService implements MqttCallback {

    private static final String BROKER_CLOUD = "tcp://mosquitto_cloud:1883";
    private static final String BROKER_FOG = "tcp://mosquitto_fog:1883";

    private static final String TOPICO_ENTRADA = "dados_processados/todos";

    private MqttClient clientCloud; // Cliente para escutar do broker cloud
    private MqttClient clientFog;   // Cliente para publicar no broker fog

    @PostConstruct
    public void start() {
        try {
            // Inicializa cliente cloud (receptor)
            clientCloud = new MqttClient(BROKER_CLOUD, MqttClient.generateClientId());
            MqttConnectOptions optionsCloud = new MqttConnectOptions();
            optionsCloud.setAutomaticReconnect(true);
            optionsCloud.setCleanSession(true);
            clientCloud.setCallback(this);
            clientCloud.connect(optionsCloud);
            clientCloud.subscribe(TOPICO_ENTRADA);
            System.out.println("Conectado ao broker CLOUD e inscrito em " + TOPICO_ENTRADA);

            // Inicializa cliente fog (publicador)
            clientFog = new MqttClient(BROKER_FOG, MqttClient.generateClientId());
            MqttConnectOptions optionsFog = new MqttConnectOptions();
            optionsFog.setAutomaticReconnect(true);
            optionsFog.setCleanSession(true);
            clientFog.connect(optionsFog);
            System.out.println("Conectado ao broker FOG para publicação");

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        System.out.println("Mensagem recebida de CLOUD em '" + topic + "': " + payload);

        try {
            String[] partes = payload.split("]");
            String regiao = partes[0].replace("[", "").trim();
            String valoresBrutos = partes[1].replace("[", "").trim();

            String[] valores = valoresBrutos.split("\\|");
            List<Double> numeros = Arrays.stream(valores)
                .map(v -> v.replace(",", ".").trim())
                .map(Double::parseDouble)
                .toList();

            String topicoCliente = "cliente/" + regiao.toLowerCase();
            String novaMensagem = "Dados da região " + regiao + ": " + numeros;

            // Publica no FOG
            clientFog.publish(topicoCliente, new MqttMessage(novaMensagem.getBytes()));
            clientFog.publish("cliente/todos", new MqttMessage(novaMensagem.getBytes()));
            System.out.println("Publicado no FOG em '" + topicoCliente + "': " + novaMensagem);

        } catch (Exception e) {
            System.err.println("Erro ao processar mensagem: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("Conexão perdida com o broker CLOUD: " + cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}