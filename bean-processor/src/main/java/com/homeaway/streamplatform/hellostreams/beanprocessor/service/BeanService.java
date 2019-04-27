package com.homeaway.streamplatform.hellostreams.beanprocessor.service;

import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.beanprocessor.dao.BeanDao;
import com.homeaway.streamplatform.hellostreams.beanprocessor.model.BeanSupplied;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BeanService {
    private BeanDao beanDao;

    @Autowired
    public BeanService(BeanDao beanDao) {
        Preconditions.checkNotNull(beanDao, "beanDao cannot be null");

        this.beanDao = beanDao;
    }

    public Integer getAvailableBeans() {
        return beanDao.getNumBeans();
    }

    public BeanSupplied supplyBeans(String actorId, int numBeans) {
        return beanDao.supplyBeans(actorId, numBeans);
    }
}
