package com.homeaway.streamplatform.hellostreams.orderprocessor.dao;

import com.homeaway.streamplatform.hellostreams.orderprocessor.model.BeansSupplied;
import org.springframework.stereotype.Repository;

import java.beans.Beans;
import java.time.ZonedDateTime;
import java.util.UUID;

@Repository
public class SupplyDao {
    // cached for now.. when persistent use actual persistence
    private int availableBeans = 50;

    public int getAvailableBeans() {
        return availableBeans;
    }

    public BeansSupplied supplyBeans(int numBeans) {
        BeansSupplied beansSupplied = createBeansSupplied(numBeans);
        availableBeans += numBeans;
        return beansSupplied;
    }

    private BeansSupplied createBeansSupplied(int numBeans) {
        BeansSupplied beansSupplied = new BeansSupplied();
        beansSupplied.setId(UUID.randomUUID().toString());
        beansSupplied.setNumBeansAdded(numBeans);
        beansSupplied.setCreated(ZonedDateTime.now());
        return beansSupplied;
    }
}
