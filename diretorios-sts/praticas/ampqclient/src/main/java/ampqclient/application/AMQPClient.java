package ampqclient.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class AMQPClient {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        // Passo 1: Descobrir endereço do amqpservice via API Gateway
        URI uri = new URI("http://localhost:8080/gateway/rabbitmq");
        URL url = uri.toURL();
        HttpURLConnection connGateway = (HttpURLConnection) url.openConnection();
        connGateway.setRequestMethod("GET");

        if (connGateway.getResponseCode() != 200) {
            System.out.println("Não foi possível localizar o serviço RabbitMQ.");
            scanner.close();
            return;
        }

        String respostaGateway = lerResposta(connGateway.getInputStream());
        JSONObject jsonGateway = new JSONObject(respostaGateway);
        String amqpServiceHost = jsonGateway.getString("host");
        int amqpServicePort = jsonGateway.getInt("port");

        // Passo 2: Consultar o amqpservice para obter o broker real
        String brokerURL = "http://" + amqpServiceHost + ":" + amqpServicePort + "/amqpservice/broker";
        HttpURLConnection connBroker = (HttpURLConnection) new URL(brokerURL).openConnection();
        connBroker.setRequestMethod("GET");

        if (connBroker.getResponseCode() != 200) {
            System.out.println("Não foi possível obter o endereço do broker RabbitMQ.");
            scanner.close();
            return;
        }

        String respostaBroker = lerResposta(connBroker.getInputStream());
        JSONObject jsonBroker = new JSONObject(respostaBroker);
        String brokerHost = jsonBroker.getString("host");
        int brokerPort = jsonBroker.getInt("port");

        System.out.println("Broker RabbitMQ localizado em: " + brokerHost + ":" + brokerPort);

        // Escolha de modo
        System.out.println("Escolha uma opção:");
        System.out.println("1 - Ver dados em tempo real");
        System.out.println("2 - Consultar histórico salvo no AMQPService");
        System.out.print("Opção: ");
        int escolha = scanner.nextInt();
        scanner.nextLine();

        if (escolha == 1) {
            iniciarModoTempoReal(scanner, brokerHost, brokerPort);
        } else if (escolha == 2) {
            consultarHistoricoViaHttp(amqpServiceHost, amqpServicePort);
        } else {
            System.out.println("Opção inválida.");
        }

        scanner.close();
    }

    private static void iniciarModoTempoReal(Scanner scanner, String host, int port) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);

        System.out.println("Escolha a fila de cliente para receber dados:");
        System.out.println("1 - cliente_todos");
        System.out.println("2 - cliente_norte");
        System.out.println("3 - cliente_sul");
        System.out.println("4 - cliente_leste");
        System.out.println("5 - cliente_oeste");
        System.out.print("Opção: ");
        int opcao = scanner.nextInt();
        scanner.nextLine();

        String queueName;
        switch (opcao) {
            case 2: queueName = "cliente_norte"; break;
            case 3: queueName = "cliente_sul"; break;
            case 4: queueName = "cliente_leste"; break;
            case 5: queueName = "cliente_oeste"; break;
            default: queueName = "cliente_todos"; break;
        }

        com.rabbitmq.client.Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(queueName, true, false, false, null);

        System.out.println("Aguardando mensagens da fila: " + queueName);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String dado = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("Recebido: " + dado);
        };

        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
    }

    private static void consultarHistoricoViaHttp(String host, int port) {
        try {
            String urlHistorico = "http://" + host + ":" + port + "/amqpservice/historico";
            HttpURLConnection conn = (HttpURLConnection) new URL(urlHistorico).openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != 200) {
                System.out.println("Erro ao consultar histórico.");
                return;
            }

            String resposta = lerResposta(conn.getInputStream());
            JSONArray dados = new JSONArray(resposta);

            System.out.println("=== Histórico do AMQPService ===");
            for (int i = 0; i < dados.length(); i++) {
                System.out.println(dados.getString(i));
            }

        } catch (Exception e) {
            System.out.println("Erro ao consultar histórico: " + e.getMessage());
        }
    }

    private static String lerResposta(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        StringBuilder resposta = new StringBuilder();
        String linha;
        while ((linha = reader.readLine()) != null) {
            resposta.append(linha);
        }
        reader.close();
        return resposta.toString();
    }
}



