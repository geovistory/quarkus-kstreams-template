package org.acme.kafka.streams.aggregator.streams;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.quarkus.logging.Log;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.acme.avro.Aggregation;
import org.acme.avro.TemperatureValue;
import org.acme.avro.WheatherStationKey;
import org.acme.avro.WheatherStationValue;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.streams.KafkaStreams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.acme.kafka.streams.aggregator.streams.TopologyProducer.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

/**
 * Integration testing of the application with an embedded broker.
 * Remark: KafkaStreams and Redpanda is not restarted between tests.
 * With multiple test in the same test class, test can affect each other.
 */
@QuarkusTest
@QuarkusTestResource(RedpandaResource.class)
public class TopologyProducerTest {

    @Inject
    KafkaStreams kafkaStreams;

    @InjectRedpandaResource
    RedpandaResource redpandaResource;

    KafkaProducer<WheatherStationKey, TemperatureValue> temperatureProducer;

    KafkaProducer<WheatherStationKey, WheatherStationValue> weatherStationsProducer;

    KafkaConsumer<WheatherStationKey, Aggregation> aggregationConsumer;

    KafkaConsumer<WheatherStationKey, WheatherStationValue> weatherStationsConsumer;


    @BeforeEach
    public void setUp() {

        temperatureProducer = new KafkaProducer<>(producerProps());
        weatherStationsProducer = new KafkaProducer<>(producerProps());
        aggregationConsumer = new KafkaConsumer<>(consumerProps());
        weatherStationsConsumer = new KafkaConsumer<>(consumerProps());

    }

    @AfterEach
    public void tearDown() {
        temperatureProducer.close();
        weatherStationsProducer.close();
        aggregationConsumer.close();
        weatherStationsConsumer.close();
        kafkaStreams.close();
        Log.info("clean up state directory");
        kafkaStreams.cleanUp();
    }

    @Test
    @Timeout(value = 30)
    public void testAggregation() {
        var wheatherStationKey = WheatherStationKey.newBuilder().setId(2).build();

        aggregationConsumer.subscribe(Collections.singletonList(TEMPERATURES_AGGREGATED_TOPIC));
        weatherStationsProducer.send(new ProducerRecord<>(WEATHER_STATIONS_TOPIC,
                wheatherStationKey,
                WheatherStationValue.newBuilder().setName("Station 1").build())
        );
        temperatureProducer.send(new ProducerRecord<>(TEMPERATURE_VALUES_TOPIC,
                wheatherStationKey,
                TemperatureValue.newBuilder().setTemperature(Instant.now() + ";" + "15").build())
        );
        temperatureProducer.send(new ProducerRecord<>(TEMPERATURE_VALUES_TOPIC,
                wheatherStationKey,
                TemperatureValue.newBuilder().setTemperature(Instant.now() + ";" + "25").build())
        );

        List<ConsumerRecord<WheatherStationKey, Aggregation>> results = poll(aggregationConsumer, 1);

        // Assumes the state store was initially empty
        assertEquals(2, results.get(0).value().getCount());
        assertEquals(2, results.get(0).key().getId());
        assertEquals("Station 1", results.get(0).value().getStationName());
        assertEquals(20, results.get(0).value().getAvg());
    }

    private Properties consumerProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, redpandaResource.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-id");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        props.put("schema.registry.url", redpandaResource.getSchemaRegistryAddress());
        return props;
    }

    private Properties producerProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, redpandaResource.getBootstrapServers());
        props.put("schema.registry.url", redpandaResource.getSchemaRegistryAddress());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                io.confluent.kafka.serializers.KafkaAvroSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                io.confluent.kafka.serializers.KafkaAvroSerializer.class);
        return props;
    }

    private List<ConsumerRecord<WheatherStationKey, Aggregation>> poll(Consumer<WheatherStationKey, Aggregation> consumer, int expectedRecordCount) {
        int fetched = 0;
        List<ConsumerRecord<WheatherStationKey, Aggregation>> result = new ArrayList<>();
        while (fetched < expectedRecordCount) {
            ConsumerRecords<WheatherStationKey, Aggregation> records = consumer.poll(Duration.ofSeconds(1));
            records.forEach(result::add);
            fetched = result.size();
        }
        return result;
    }
}