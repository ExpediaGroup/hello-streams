package com.homeaway.streamplatform.hellostreams.beanprocessor.resolvers;

import com.coxautodev.graphql.tools.GraphQLQueryResolver;
import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.beanprocessor.service.BaristaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@SuppressWarnings("unused")
@Component
public class Query implements GraphQLQueryResolver {
    private final BaristaService baristaService;

    @Autowired
    public Query(BaristaService baristaService) {
        Preconditions.checkNotNull(baristaService, "baristaService cannot be null");

        this.baristaService = baristaService;
    }

    public Boolean isProcessing() { return baristaService.isProcessing(); }
}
