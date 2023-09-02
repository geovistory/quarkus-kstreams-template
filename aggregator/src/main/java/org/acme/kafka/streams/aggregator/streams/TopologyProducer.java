package org.acme.kafka.streams.aggregator.streams;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.acme.avro.*;
import org.acme.kafka.streams.aggregator.ConfiguredAvroSerde;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.Stores;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@ApplicationScoped
public class TopologyProducer {

    static final String WEATHER_STATIONS_STORE = "weather-stations-store";

    private static final String WEATHER_STATIONS_TOPIC = "weather-stations";
    private static final String TEMPERATURE_VALUES_TOPIC = "temperature-values";
    private static final String TEMPERATURES_AGGREGATED_TOPIC = "temperatures-aggregated";

    @Inject
    ConfiguredAvroSerde as;

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        KeyValueBytesStoreSupplier storeSupplier = Stores.persistentKeyValueStore(
                WEATHER_STATIONS_STORE);

        GlobalKTable<WheatherStationKey, WheatherStationValue> stations = builder.globalTable(
                WEATHER_STATIONS_TOPIC,
                Consumed.with(as.key(), as.value()));

        builder.stream(
                        TEMPERATURE_VALUES_TOPIC,
                        Consumed.with(as.<WheatherStationKey>key(), as.<TemperatureValue>value())
                )
                .join(
                        stations,
                        (stationId, timestampAndValue) -> stationId,
                        (timestampAndValue, station) -> {
                            String[] parts = timestampAndValue.getTemperature().split(";");
                            return TemperatureMeasurement.newBuilder()
                                    .setStationName(station.getName())
                                    .setTimestamp(Instant.parse(parts[0]).toString())
                                    .setValue(Double.parseDouble(parts[1]))
                                    .build();
                        }
                )
                .groupByKey()
                .aggregate(
                        () -> Aggregation.newBuilder()
                                .setStationName("")
                                .setMin(Double.MAX_VALUE).setMax(Double.MAX_VALUE).build(),
                        (stationId, value, aggregation) -> updateAggregation(aggregation, value),
                        Materialized.<WheatherStationKey, Aggregation>as(storeSupplier)
                                .withKeySerde(as.key())
                                .withValueSerde(as.value())
                )
                .toStream()
                .to(
                        TEMPERATURES_AGGREGATED_TOPIC,
                        Produced.with(as.key(), as.value())
                );

        return builder.build();
    }

    static Aggregation updateAggregation(Aggregation a, TemperatureMeasurement measurement) {
        a.setStationName(measurement.getStationName());
        a.setCount(a.getCount() + 1);
        a.setSum(a.getSum() + measurement.getValue());
        a.setAvg(BigDecimal.valueOf(a.getSum() / a.getCount())
                .setScale(1, RoundingMode.HALF_UP).doubleValue());

        a.setMin(Math.min(a.getMin(), measurement.getValue()));
        a.setMax(Math.max(a.getMax(), measurement.getValue()));
        return a;
    }
}