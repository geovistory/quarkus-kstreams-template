package org.acme.kafka.streams.aggregator.streams;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.testcontainers.redpanda.RedpandaContainer;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class RedpandaResource implements QuarkusTestResourceLifecycleManager {

    private RedpandaContainer container = new RedpandaContainer("docker.redpanda.com/redpandadata/redpanda:v23.1.2");
    @Override
    public Map<String, String> start() {
        container.start();
        var props = new HashMap<String, String>();
        props.put("quarkus.kafka-streams.bootstrap-servers", container.getBootstrapServers());
        props.put("quarkus.kafka-streams.schema.registry.url", container.getSchemaRegistryAddress());
        //props.put("schema.registry.url", container.getSchemaRegistryAddress());
        return props;
    }

    @Override
    public void stop() {
        container.close();
    }

    public String getBootstrapServers() {
        return container.getBootstrapServers();
    }

    public String getSchemaRegistryAddress() {
        return container.getSchemaRegistryAddress();
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(this, new TestInjector.AnnotatedAndMatchesType(InjectRedpandaResource.class, RedpandaResource.class));
    }

    public void deleteTopics() {
        // Set the Kafka broker properties
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers()); // Replace with your broker address
        properties.put(AdminClientConfig.CLIENT_ID_CONFIG, "KafkaTopicDeletion");

        // Create an AdminClient
        try (AdminClient adminClient = AdminClient.create(properties)) {
            // List all topics
            var topicNames = adminClient.listTopics().names().get();

            // Delete each topic
            List<String> topicsToDelete = new ArrayList<>();
            for (String topic : topicNames) {
                // Exclude internal topics
                if (!topic.startsWith("__") && !topic.startsWith("_schema")) {
                    topicsToDelete.add(topic);
                }
            }

            if (!topicsToDelete.isEmpty()) {
                System.out.println("Deleting topics: " + topicsToDelete);
                DeleteTopicsResult deleteTopicsResult = adminClient.deleteTopics(topicsToDelete);

                // Wait for topic deletion to complete
                for (Map.Entry<String, KafkaFuture<Void>> entry : deleteTopicsResult.topicNameValues().entrySet()) {
                    try {
                        entry.getValue().get();
                        System.out.println("Topic " + entry.getKey() + " deleted successfully");
                    } catch (InterruptedException | ExecutionException e) {
                        System.err.println("Failed to delete topic " + entry.getKey() + ": " + e.getMessage());
                    }
                }
            } else {
                System.out.println("No topics to delete.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
