package org.acme.kafka.streams.aggregator;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serde;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;

import java.util.HashMap;

@ApplicationScoped
public class ConfiguredAvroSerde {

    @ConfigProperty(name = "schema.registry.url")
    public String schemaRegistryUrl;

    private Serde configuredKeySerde;
    private Serde configuredValueSerde;

    public <T extends SpecificRecord> Serde<T> key() {
        if (configuredKeySerde != null) return configuredKeySerde;
        var properties = new HashMap<>();
        properties.put("schema.registry.url", schemaRegistryUrl);
        configuredKeySerde = new SpecificAvroSerde<>();
        configuredKeySerde.configure(properties, true);
        return configuredKeySerde;
    }

    public <T extends SpecificRecord> Serde<T> value() {
        if (configuredValueSerde != null) return configuredValueSerde;
        var properties = new HashMap<>();
        properties.put("schema.registry.url", schemaRegistryUrl);
        configuredValueSerde = new SpecificAvroSerde<>();
        configuredValueSerde.configure(properties, false);
        return configuredValueSerde;
    }

}
