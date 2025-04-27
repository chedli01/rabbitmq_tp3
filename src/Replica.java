import com.rabbitmq.client.*;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Replica {
    public static void main(String[] argv) throws Exception {
        if (argv.length != 1) {
            System.err.println("Usage: java Replica <ReplicaNumber>");
            System.exit(1);
        }

        String replicaNumber = argv[0];
        String queueName = "replica" + replicaNumber + "_queue";
        String fileName = "rep" + replicaNumber + "_fichier.txt";
        String responseQueue = "response_queue_" + replicaNumber;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        Channel responseChannel = connection.createChannel();

        channel.queueDeclare(queueName, false, false, false, null);
        responseChannel.queueDeclare(responseQueue, false, false, false, null);

        System.out.println("Replica " + replicaNumber + " en attente de messages...");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");

            if (message.equals("ReadLast")) {
                // Envoyer la dernière ligne
                List<String> lines = Files.readAllLines(Paths.get(fileName));
                String lastLine = lines.size() > 0 ? lines.get(lines.size() - 1) : "Fichier Vide";
                responseChannel.basicPublish("", "client_reader_queue", null, lastLine.getBytes());
                System.out.println("Replica " + replicaNumber + " répond: " + lastLine);
            } else if (message.equals("ReadAll")) {
                // Envoyer tout le fichier ligne par ligne
                List<String> lines = Files.readAllLines(Paths.get(fileName));
                for (String line : lines) {
                    responseChannel.basicPublish("", "client_reader_queue", null, line.getBytes());
                }
                responseChannel.basicPublish("", "client_reader_queue", null, "EOF".getBytes());
                System.out.println("Replica " + replicaNumber + " a envoyé tout son fichier.");
            } else {
                try (FileWriter writer = new FileWriter(fileName, true)) {
                    writer.write(message + "\n");
                    System.out.println("Replica " + replicaNumber + " a écrit: " + message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
    }
}
