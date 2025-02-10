package demo.runme;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class RunMe {

    @Test
    void demoGenericContainer () {
        var postgres     = new GenericContainer<>(DockerImageName.parse("postgres:16.2"))
                 .withExposedPorts(5432)
                 .withEnv("POSTGRES_DB", "testdb")
                 .withEnv("POSTGRES_USER", "user")
                 .withEnv("POSTGRES_PASSWORD", "password");
        postgres.start();

        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/testdb",
                postgres.getHost(),
                postgres.getMappedPort(5432));

        System.out.println("PostgreSQL is ready!");
        System.out.println("JDBC URL: " + jdbcUrl);
    }

    @Test
    void demoModule () {
        var postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
        postgres.start();
        System.out.println("JDBC URL: " + postgres.getJdbcUrl());
    }

    @Test
    void demoSimpleCluster() throws Exception {
        // Create network and two Kafka brokers
        Network network = Network.newNetwork();

        KafkaContainer kafka1 = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"))
                .withNetwork(network)
                .withNetworkAliases("kafka1");

        KafkaContainer kafka2 = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"))
                .withNetwork(network)
                .withNetworkAliases("kafka2");

        // Start the containers
        kafka1.start();
        kafka2.start();

        String bootstrapServers = String.format("%s,%s",
                kafka1.getBootstrapServers(),
                kafka2.getBootstrapServers());

        System.out.println("Kafka Cluster is ready!");
        System.out.println("Bootstrap Servers: " + bootstrapServers);

        // Create producer
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", bootstrapServers);
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        // Create consumer
        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", bootstrapServers);
        consumerProps.put("group.id", "demo-group");
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("auto.offset.reset", "earliest");

        // Create topic name
        String topic = "demo-topic";

        // Emulate messaging
        try (
                KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);
                KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)
        ) {
            // Subscribe consumer
            consumer.subscribe(Collections.singletonList(topic));

            // Send a message
            String message = "Hello Kafka Cluster!";
            producer.send(new ProducerRecord<>(topic, message)).get();
            System.out.println("\nSent message: " + message);

            // Receive the message
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            records.forEach(record ->
                    System.out.println("Received message: " + record.value() +
                            " from partition: " + record.partition())
            );
        }

        // Cleanup
        kafka2.stop();
        kafka1.stop();
        network.close();
    }

}
