package mqttclient.application;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.json.JSONObject;

public class ClienteMQTT {

    public static void main(String[] args) {
        try {
            // Passo 1: Descobrir host e porta do mqttservice via API Gateway
        	URI uri = new URI("http://localhost:8080/gateway/mqtt");
        	URL url = uri.toURL();
            HttpURLConnection connGateway = (HttpURLConnection) url.openConnection();
            connGateway.setRequestMethod("GET");

            if (connGateway.getResponseCode() != 200) {
                System.out.println("Não foi possível localizar o serviço MQTT.");
                return;
            }

            InputStream inGateway = connGateway.getInputStream();
            BufferedReader readerGateway = new BufferedReader(new InputStreamReader(inGateway));
            StringBuilder respGateway = new StringBuilder();
            String line;
            while ((line = readerGateway.readLine()) != null) {
                respGateway.append(line);
            }
            JSONObject jsonGateway = new JSONObject(respGateway.toString());
            String mqttHost = jsonGateway.getString("host");
            String mqttPort = jsonGateway.getString("port");

            // Passo 2: Consultar o mqttservice para obter o broker real
            String urlBrokerStr = "http://" + mqttHost + ":" + mqttPort + "/mqttservice/broker";
            URL urlBroker = new URL(urlBrokerStr);
            HttpURLConnection connBroker = (HttpURLConnection) urlBroker.openConnection();
            connBroker.setRequestMethod("GET");

            if (connBroker.getResponseCode() != 200) {
                System.out.println("Não foi possível obter o endereço do broker MQTT.");
                return;
            }

            InputStream inBroker = connBroker.getInputStream();
            BufferedReader readerBroker = new BufferedReader(new InputStreamReader(inBroker));
            StringBuilder respBroker = new StringBuilder();
            while ((line = readerBroker.readLine()) != null) {
                respBroker.append(line);
            }
            JSONObject jsonBroker = new JSONObject(respBroker.toString());
            String brokerUrl = jsonBroker.getString("brokerUrl");

            System.out.println("Broker MQTT localizado em: " + brokerUrl);

            // Passo 3: Escolher tópico e conectar no broker real
            Scanner scanner = new Scanner(System.in);
            System.out.println("Escolha o tópico para receber dados:");
            System.out.println("1 - Todos");
            System.out.println("2 - Norte");
            System.out.println("3 - Sul");
            System.out.println("4 - Leste");
            System.out.println("5 - Oeste");
            System.out.print("Opção: ");
            int opcao = scanner.nextInt();
            scanner.close();

            String topic;
            switch (opcao) {
                case 2: topic = "cliente/norte"; break;
                case 3: topic = "cliente/sul"; break;
                case 4: topic = "cliente/leste"; break;
                case 5: topic = "cliente/oeste"; break;
                default: topic = "cliente/todos"; break;
            }

            MqttClient client = new MqttClient(brokerUrl, MqttClient.generateClientId());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);

            client.connect(options);
            System.out.println("Consumidor MQTT conectado ao broker.");
            System.out.println("Inscrito no tópico: " + topic);

            client.subscribe(topic, (t, msg) -> {
                System.out.println("Mensagem recebida [MQTT]: " + new String(msg.getPayload()));
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


