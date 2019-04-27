package com.homeaway.streamplatform.hellostreams.beanprocessor.dao;

import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.BeanSupply;
import com.homeaway.streamplatform.hellostreams.beanprocessor.BeanProcessorUtils;
import com.homeaway.streamplatform.hellostreams.beanprocessor.model.BeanSupplied;
import com.homeaway.streamplatform.hellostreams.beanprocessor.processor.BeanProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.homeaway.streamplatform.hellostreams.beanprocessor.processor.BeanProcessor.GLOBAL_KEY;

@Repository
@Slf4j
@DependsOn({"beanProcessor"})
public class BeanDao {
    @Value("${processor.bean.commands.stream}")
    private String beanCommandsStream;

    @Value("${processor.write.wait.timeout}")
    private long writeWaitTimeout;

    private KafkaProducer<String, com.homeaway.streamplatform.hellostreams.BeanSupplied> kafkaBeanEventProducer;
    private BeanProcessor beanProcessor;

    @Autowired
    public BeanDao(KafkaProducer<String, com.homeaway.streamplatform.hellostreams.BeanSupplied> kafkaBeanEventProducer,
                   BeanProcessor beanProcessor) {
        Preconditions.checkNotNull(kafkaBeanEventProducer, "kafkaBeanEventProducer cannot be null");
        Preconditions.checkNotNull(beanProcessor, "beanProcessor cannot be null");

        this.kafkaBeanEventProducer = kafkaBeanEventProducer;
        this.beanProcessor = beanProcessor;
    }

    public int getNumBeans() {
        BeanSupply beanSupply = beanProcessor.getBeanSupply(GLOBAL_KEY);
        if(beanSupply == null) {
            log.info("BeanSupply has not received any command events yet, returning 0 beansAvailable");
            return 0;
        }
        return beanSupply.getBeansAvailable();
    }

    public BeanSupplied supplyBeans(String actorId, int numBeans) {
        com.homeaway.streamplatform.hellostreams.BeanSupplied beanSupplied =
                createBeanSuppliedEvent(actorId, numBeans);

        send(beanSupplied);

        return toDTO(beanSupplied);
    }

    private void send(com.homeaway.streamplatform.hellostreams.BeanSupplied beanSuppliedEvent) {
        try {
            log.info("Writing beanSupplied={} to kafka.", beanSuppliedEvent);
            Future<RecordMetadata> resultFuture = kafkaBeanEventProducer.send(createProducerRecord(beanSuppliedEvent));
            // sync wait for response
            resultFuture.get(writeWaitTimeout, TimeUnit.MILLISECONDS);

            // read your writes!!
            waitForWrite(GLOBAL_KEY, beanSuppliedEvent.getId());
        } catch (Exception e) {
            throw new IllegalStateException("Could not write to kafka.", e);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void waitForWrite(String key, String commandEventId) {
        // TODO - move hardcode wait time into property
        long timeout = System.currentTimeMillis() + 30000;
        boolean found;
        do {
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
            BeanSupply beanSupply = beanProcessor.getBeanSupply(key);
            found = (beanSupply!=null && commandEventId.equals(beanSupply.getId()));
        } while(System.currentTimeMillis() < timeout && !found);
    }

    private ProducerRecord<String,com.homeaway.streamplatform.hellostreams.BeanSupplied> createProducerRecord(com.homeaway.streamplatform.hellostreams.BeanSupplied beanSuppliedEvent) {
        return new ProducerRecord<>(
                beanCommandsStream, null,
                beanSuppliedEvent.getCreated().getMillis(), // timestamp
                GLOBAL_KEY, // use fixed key so that all bean supply events get aggregated into same partition
                beanSuppliedEvent);
    }

    private BeanSupplied toDTO(com.homeaway.streamplatform.hellostreams.BeanSupplied beanSupplied) {
        BeanSupplied dto = new BeanSupplied();
        dto.setId(beanSupplied.getId());
        dto.setActorId(beanSupplied.getActorId());
        dto.setBeansSupplied(beanSupplied.getBeansSupplied());
        dto.setCreated(toDTOTime(beanSupplied.getCreated()));
        return dto;
    }

    private ZonedDateTime toDTOTime(DateTime avroTime) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(avroTime.getMillis()),
                BeanProcessorUtils.UTC_ZONE_ID);
    }

    private com.homeaway.streamplatform.hellostreams.BeanSupplied createBeanSuppliedEvent(String actorId, int numBeans) {
        return com.homeaway.streamplatform.hellostreams.BeanSupplied.newBuilder()
        .setId(UUID.randomUUID().toString())
        .setActorId(actorId)
        .setBeansSupplied(numBeans)
        .setCreated(DateTime.now())
        .build();
    }
}

