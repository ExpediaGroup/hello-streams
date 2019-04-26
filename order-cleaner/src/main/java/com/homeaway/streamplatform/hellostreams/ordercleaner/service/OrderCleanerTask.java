package com.homeaway.streamplatform.hellostreams.ordercleaner.service;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class OrderCleanerTask {

    private final ExecutorService executor;
    private OrderCleanerService orderCleanerService;
    private long nextStartTime = 0;

    @Value("${processor.cleaner.sleep.ms}")
    private long cleanerSleepMillis;

    @Value("${processor.cleaner.interval.ms}")
    private long cleanIntervalMillis;


    public OrderCleanerTask(@Autowired OrderCleanerService orderCleanerService) {
        Preconditions.checkNotNull(orderCleanerService, "orderCleanerService cannot be null");
        this.orderCleanerService = orderCleanerService;
        this.executor = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setDaemon(false).setNameFormat("cleaner-%d").build());
    }

    @PostConstruct
    public void start() {
        // run now
        log.info("Starting order cleaner");
        nextStartTime += cleanIntervalMillis;
        runOnce();
        log.info("Order cleaner started");
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping order cleaner");
        executor.shutdownNow();
        nextStartTime = 0;
        log.info("Order cleaner stopped");
    }

    public void runOnce() {
        executor.submit(this::cleanOrders);
    }

    public void cleanOrders() {
        // wait for clean
        waitForClean();

        // clean orders
        orderCleanerService.cleanOrders();

        // continuation
        executor.submit(this::cleanOrders);
    }

    public DateTime getNextStartTime() {
        return new DateTime(nextStartTime);
    }

    private void waitForClean() {
        log.info("Waiting for clean...");
        while(System.currentTimeMillis() < nextStartTime) {
            try { Thread.sleep(cleanerSleepMillis); } catch (InterruptedException ignore) {}
        }
        // setup for next time
        nextStartTime += cleanIntervalMillis;
    }

}
