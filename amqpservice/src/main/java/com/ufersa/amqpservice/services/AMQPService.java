package com.ufersa.amqpservice.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.stereotype.Service;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import jakarta.annotation.PostConstruct;

@Service
public class AMQPService {

    private static final String ARQUIVO_DADOS = "rabbitmq/dados_recebidos.txt";

    private static final String CLOUD_HOST = "rabbitmq_cloud"; 
    private static final int CLOUD_PORT = 5672;

    private static final String FOG_HOST = "rabbitmq_fog"; 
    private static final int FOG_PORT = 5672;

    @PostConstruct
    public void iniciar() {
        try {
            // 🔌 Conexão com broker cloud
            ConnectionFactory cloudFactory = new ConnectionFactory();
            cloudFactory.setHost(CLOUD_HOST);
            cloudFactory.setPort(CLOUD_PORT);
            Connection cloudConnection = cloudFactory.newConnection();
            Channel cloudChannel = cloudConnection.createChannel();

            // 🔌 Conexão com broker fog
            ConnectionFactory fogFactory = new ConnectionFactory();
            fogFactory.setHost(FOG_HOST);
            fogFactory.setPort(FOG_PORT);
            Connection fogConnection = fogFactory.newConnection();
            Channel fogChannel = fogConnection.createChannel();

            String fila = "dados_processados_todos";
            cloudChannel.queueDeclare(fila, true, false, false, null);

            cloudChannel.basicConsume(fila, true, (consumerTag, delivery) -> {
                String dado = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("Recebido [" + fila + "]: " + dado);

                salvarEmArquivo(dado);
                publicarParaCliente(fogChannel, dado); 

            }, consumerTag -> {});

            System.out.println("AMQPService escutando fila do broker CLOUD: " + fila);

        } catch (Exception e) {
            System.err.println("Erro ao iniciar AMQPService: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void publicarParaCliente(Channel channel, String dado) {
        try {
            String[] partes = dado.split("]");
            String regiao = partes[0].replace("[", "").trim().toLowerCase();

            String filaTodos = "cliente_todos";
            String filaRegiao = "cliente_" + regiao;

            channel.queueDeclare(filaTodos, true, false, false, null);
            channel.queueDeclare(filaRegiao, true, false, false, null);

            channel.basicPublish("", filaTodos, null, dado.getBytes(StandardCharsets.UTF_8));
            channel.basicPublish("", filaRegiao, null, dado.getBytes(StandardCharsets.UTF_8));

            System.out.printf("Publicado no FOG: '%s' e '%s': %s%n", filaTodos, filaRegiao, dado);
        } catch (Exception e) {
            System.err.println("Erro ao publicar para cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void salvarEmArquivo(String dado) {
        File dir = new File("rabbitmq");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File arquivo = new File(dir, "dados_recebidos.txt");

        try (FileWriter fw = new FileWriter(arquivo, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            out.println(dado);

        } catch (IOException e) {
            System.err.println("Erro ao salvar no arquivo: " + e.getMessage());
        }
    }

    public List<String> lerHistorico() {
        File arquivo = new File(ARQUIVO_DADOS);
        if (!arquivo.exists()) return List.of();

        try (BufferedReader reader = new BufferedReader(new FileReader(arquivo))) {
            return reader.lines().toList();
        } catch (IOException e) {
            System.err.println("Erro ao ler histórico: " + e.getMessage());
            return List.of();
        }
    }
}


