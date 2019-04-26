package com.homeaway.streamplatform.hellostreams.beanprocessor.dao;

import com.homeaway.streamplatform.hellostreams.beanprocessor.model.BeanSupplied;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.UUID;

@Repository
public class BeanDao {
    private Integer numBeans = 50;

    public Integer getNumBeans() {
        // FIXME delegate to processor
        return numBeans;
    }

    private void addBeans(int numBeans) {
        // FIXME delete this method once processor is in place
        this.numBeans += numBeans;
    }

    public BeanSupplied supplyBeans(int numBeans) {
        BeanSupplied beanSupplied = createBeanSupplied(numBeans);

        // FIXME write command event
        // FIXME wait for command event to propagate
        addBeans(numBeans);

        // FIXME toDTO method to convert command event to DTO
        return beanSupplied;
    }

    private BeanSupplied createBeanSupplied(int numBeans) {
        BeanSupplied beanSupplied = new BeanSupplied();
        beanSupplied.setBeansSupplied(numBeans);
        beanSupplied.setId(UUID.randomUUID().toString());
        beanSupplied.setCreated(ZonedDateTime.now());
        return beanSupplied;
    }
}

