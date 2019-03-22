package com.homeaway.streamplatform.hellostreams.orderprocessor.service;

import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.orderprocessor.dao.SupplyDao;
import com.homeaway.streamplatform.hellostreams.orderprocessor.model.BeansSupplied;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SupplyService {
    private SupplyDao supplyDao;

    public SupplyService(@Autowired SupplyDao supplyDao) {
        Preconditions.checkNotNull(supplyDao, "supplyDao cannot be null");
        this.supplyDao = supplyDao;
    }
    public int getAvailableBeans() {
        return supplyDao.getAvailableBeans();
    }

    public BeansSupplied supplyBeans(int numBeans) {
        return supplyDao.supplyBeans(numBeans);
    }
}
