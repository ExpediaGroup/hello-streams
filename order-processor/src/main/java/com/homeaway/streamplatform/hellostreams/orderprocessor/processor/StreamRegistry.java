package com.homeaway.streamplatform.hellostreams.orderprocessor.processor;

import com.google.common.collect.Maps;
import com.homeaway.streamplatform.hellostreams.Order;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes.StringSerde;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;

import static io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

/**
 * This class is intended to be a shunt for a full-fledged Stream Registry
 */
@Component
@Slf4j
public class StreamRegistry {
    private Map<String,StreamMetadata> STREAMS = Maps.newHashMap();

    @Value("${order-processor.order.commands.stream}")
    private String orderCommandsStream;

    @Value("${order-processor.orders.stream}")
    private String ordersStream;

    @Value("${order-processor.schema.registry.url}")
    private String schemaRegistryUrl;

    @PostConstruct
    public void init() {
        // Use this initialization in place of a full-fledged Stream Registry
        addStream(orderCommandsStream, new StringSerde(), new SpecificAvroSerde()); // avro doesn't support type inheritance yet
        addStream(ordersStream, new StringSerde(), new SpecificAvroSerde<Order>());
    }

    public StreamMetadata lookupStream(String name) {
        return STREAMS.get(name);
    }

    private <K,V> void addStream(String topic, Serde<K> keySerde, Serde<V> valueSerde) {
        configureSerde(keySerde, true);
        configureSerde(valueSerde, false);
        StreamMetadata <K,V> streamMetadata = new StreamMetadata<>(topic, keySerde, valueSerde);
        STREAMS.put(streamMetadata.getName(), streamMetadata);
    }

    private <T> void configureSerde(Serde<T> serde, boolean isKey) {
        if(serde instanceof SpecificAvroSerde) {
            serde.configure(Collections.singletonMap(SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl), isKey);
        }
    }

    @Data
    public class StreamMetadata<K,V> {
        private String name;
        private final Serde<K> keySerde;
        private final Serde<V> valueSerde;

        private StreamMetadata(String name, Serde<K> keySerde, Serde<V> valueSerde) {
            this.name = name;
            this.keySerde = keySerde;
            this.valueSerde = valueSerde;
        }
    }
}

