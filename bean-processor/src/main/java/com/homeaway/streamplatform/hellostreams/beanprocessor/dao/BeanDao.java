package com.homeaway.streamplatform.hellostreams.beanprocessor.dao;

import org.springframework.stereotype.Repository;

@Repository
public class BeanDao {
    private static Integer numBeans = 50;

    public Integer getNumBeans() {
        return numBeans;
    }

    public static void setNumBeans(Integer numBeans) {
        BeanDao.numBeans += numBeans;
    }
}

