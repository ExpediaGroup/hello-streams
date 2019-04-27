package com.homeaway.streamplatform.hellostreams.beanprocessor.resolvers;

import com.coxautodev.graphql.tools.GraphQLQueryResolver;
import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.beanprocessor.service.BeanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@SuppressWarnings("unused")
@Component
public class Query implements GraphQLQueryResolver {
    private final BeanService beanService;

    @Autowired
    public Query(BeanService beanService) {
        Preconditions.checkNotNull(beanService, "beanService cannot be null");

        this.beanService = beanService;
    }

    public Integer getAvailableBeans() { return beanService.getAvailableBeans(); }
}
