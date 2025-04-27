import com.rabbitmq.client.*;

import java.util.*;

public class ClientReaderV2 {
    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection(); 
             Channel channel = connection.createChannel()) {

            String[] replicaQueues = {"replica1_queue", "replica2_queue", "replica3_queue"};

            // Envoyer "ReadAll" à chaque Replica
            for (String replicaQueue : replicaQueues) {
                channel.basicPublish("", replicaQueue, null, "ReadAll".getBytes());
            }

            channel.queueDeclare("client_reader_queue", false, false, false, null);

            Map<String, Integer> lineCount = new HashMap<>();

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String line = new String(delivery.getBody(), "UTF-8");
                if (!line.equals("EOF")) {
                    lineCount.put(line, lineCount.getOrDefault(line, 0) + 1);
                } else {
                    // EOF reçu, affichons les résultats majoritaires
                    System.out.println("Lignes présentes dans la majorité :");
                    for (Map.Entry<String, Integer> entry : lineCount.entrySet()) {
                        if (entry.getValue() >= 2) {
                            System.out.println(entry.getKey());
                        }
                    }
                    System.exit(0); // Fin du programme
                }
            };

            channel.basicConsume("client_reader_queue", true, deliverCallback, consumerTag -> {});

            System.out.println("ClientReaderV2 attend toutes les lignes...");
            Thread.sleep(15000); // Timeout de 15 secondes
        }
    }
}
