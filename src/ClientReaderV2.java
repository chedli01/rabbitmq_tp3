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
            int[] eofCount = {0}; // compteur de EOF reçus (besoin d'un tableau car variable utilisée en lambda)

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String line = new String(delivery.getBody(), "UTF-8");
                if (!line.equals("EOF")) {
                    lineCount.put(line, lineCount.getOrDefault(line, 0) + 1);
                } else {
                    eofCount[0]++;
                    if (eofCount[0] == 3) { // ATTENTION: attendre 3 EOF avant de finir
                        System.out.println("Lignes présentes dans la majorité :");
                        for (Map.Entry<String, Integer> entry : lineCount.entrySet()) {
                            if (entry.getValue() >= 2) { // majorité = reçu par au moins 2 replicas
                                System.out.println(entry.getKey());
                            }
                        }
                        System.exit(0);
                    }
                }
            };

            channel.basicConsume("client_reader_queue", true, deliverCallback, consumerTag -> {});

            System.out.println("ClientReaderV2 attend toutes les lignes...");
            Thread.sleep(30000); // Timeout de 30 secondes au cas où
        }
    }
}
