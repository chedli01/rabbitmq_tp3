import com.rabbitmq.client.*;

import java.io.FileWriter;
import java.io.IOException;

public class Replica {
    public static void main(String[] argv) throws Exception {
        if (argv.length != 1) {
            System.err.println("Usage: java Replica <ReplicaNumber>");
            System.exit(1);
        }

        String replicaNumber = argv[0];
        String queueName = "replica" + replicaNumber + "_queue";
        String fileName = "rep" + replicaNumber + "_fichier.txt";

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(queueName, false, false, false, null);
        System.out.println("Replica " + replicaNumber + " en attente de messages...");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            try (FileWriter writer = new FileWriter(fileName, true)) {
                writer.write(message + "\n");
                System.out.println("Replica " + replicaNumber + " écrit: " + message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
    }
}
