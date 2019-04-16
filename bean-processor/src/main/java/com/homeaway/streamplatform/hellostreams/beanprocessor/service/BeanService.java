package com.homeaway.streamplatform.hellostreams.beanprocessor.service;

import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.beanprocessor.dao.BeanDao;
import com.homeaway.streamplatform.hellostreams.beanprocessor.model.BeanSupplied;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.UUID;

@Service
public class BeanService {
    private BeanDao beanDao;


    public BeanService(@Autowired BeanDao beanDao) {
        Preconditions.checkNotNull(beanDao, "beanDao cannot be null");

        this.beanDao = beanDao;
    }

    public Integer getAvailableBeans() {
        return beanDao.getNumBeans();
    }

    public BeanSupplied supplyBeans(Integer numBeans) {
        beanDao.setNumBeans(numBeans);
        String id = UUID.randomUUID().toString();
        BeanSupplied beanSupplied = new BeanSupplied();
        beanSupplied.setNumBeansAdded(numBeans);
        beanSupplied.setId(id);
        beanSupplied.setCreated(ZonedDateTime.now());
        return beanSupplied;
    }
}
