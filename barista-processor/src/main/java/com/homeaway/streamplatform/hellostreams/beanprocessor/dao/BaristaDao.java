package com.homeaway.streamplatform.hellostreams.beanprocessor.dao;

import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.beanprocessor.processor.BaristaProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Repository;

@SuppressWarnings("Duplicates")
@Repository
@Slf4j
@DependsOn({"baristaProcessor"})
public class BaristaDao {
    private BaristaProcessor baristaProcessor;

    @Autowired
    public BaristaDao(BaristaProcessor baristaProcessor) {
        Preconditions.checkNotNull(baristaProcessor, "baristaProcessor cannot be null");

        this.baristaProcessor = baristaProcessor;
    }

    public boolean isProcessing() { return baristaProcessor.isProcessing(); }
}

