package com.homeaway.streamplatform.hellostreams.orderprocessor.dao;

import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.Serdes;
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
    public KafkaProducer<String, SpecificRecord> kafkaOrderEventProducer(Properties kafkaProperties) {
        Properties props = (Properties) kafkaProperties.clone();
        props.setProperty(CLIENT_ID_CONFIG, orderEventProducerId);
        props.setProperty(ACKS_CONFIG, "all");
        props.setProperty(VALUE_SUBJECT_NAME_STRATEGY, TopicRecordNameStrategy.class.getName());
        return new KafkaProducer<>(props);
    }

    @Bean
    public Properties kafkaProperties() {
        Properties props = new Properties();
        props.setProperty(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.setProperty(KEY_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().getClass().getName());
        props.setProperty(VALUE_SERIALIZER_CLASS_CONFIG, SpecificAvroSerializer.class.getName());
        props.setProperty(SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        props.setProperty(COMPRESSION_TYPE_CONFIG, "snappy");
        return props;
    }
}
