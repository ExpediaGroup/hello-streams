package com.homeaway.streamplatform.hellostreams.beanprocessor.service;

import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.beanprocessor.dao.BaristaDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BaristaService {
    private BaristaDao baristaDao;

    @Autowired
    public BaristaService(BaristaDao baristaDao) {
        Preconditions.checkNotNull(baristaDao, "baristaDao cannot be null");

        this.baristaDao = baristaDao;
    }

    public boolean isProcessing() { return baristaDao.isProcessing(); }
}
