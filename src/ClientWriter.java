import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class ClientWriter {
    private final static String[] QUEUES = {"replica1_queue", "replica2_queue", "replica3_queue"};

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection(); 
             Channel channel = connection.createChannel()) {

            for (String queueName : QUEUES) {
                channel.queueDeclare(queueName, false, false, false, null);
            }

            String message = argv[0];
            for (String queueName : QUEUES) {
                channel.basicPublish("", queueName, null, message.getBytes());
                System.out.println("Envoyé à " + queueName + ": '" + message + "'");
            }
        }
    }
}
