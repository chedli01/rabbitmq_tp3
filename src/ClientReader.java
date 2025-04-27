import com.rabbitmq.client.*;

public class ClientReader {
    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection(); 
             Channel channel = connection.createChannel()) {

            String[] replicaQueues = {"replica1_queue", "replica2_queue", "replica3_queue"};

            // Envoyer la requête "ReadLast" aux replicas
            for (String replicaQueue : replicaQueues) {
                channel.basicPublish("", replicaQueue, null, "ReadLast".getBytes());
            }

            // Déclarer la queue où les replicas vont répondre
            channel.queueDeclare("client_reader_queue", false, false, false, null);

            // Recevoir la première réponse
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println("ClientReader a reçu : " + message);
                System.exit(0); // Fin après la première réponse
            };

            channel.basicConsume("client_reader_queue", true, deliverCallback, consumerTag -> {});

            System.out.println("ClientReader attend une réponse...");
            Thread.sleep(10000); // Timeout sécurité 10 secondes
        }
    }
}
