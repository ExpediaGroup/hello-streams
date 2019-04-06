package com.homeaway.streamplatform.hellostreams.orderprocessor.dao;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

import static io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY;
import static org.apache.kafka.clients.producer.ProducerConfig.*;

/**
 * Configuration for kafka
 */
@Configuration
public class KafkaConfig {
    @Value("${order-processor.bootstrap.servers}")
    private String bootstrapServers;

    @Value("${order-processor.schema.registry.url}")
    private String schemaRegistryUrl;

    @Value("${order-processor.order.event.producer.id}")
    private String orderEventProducerId;

    @Bean
    public KafkaProducer<String, SpecificRecord> kafkaOrderEventProducer() {
        Properties props = getKafkaProperties();
        props.put(CLIENT_ID_CONFIG, orderEventProducerId);
        props.put(ACKS_CONFIG, "all");
        props.put(VALUE_SUBJECT_NAME_STRATEGY, TopicRecordNameStrategy.class.getName());
        return new KafkaProducer<>(props);
    }

    private Properties getKafkaProperties() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        props.put(COMPRESSION_TYPE_CONFIG, "snappy");
        return props;
    }
}
