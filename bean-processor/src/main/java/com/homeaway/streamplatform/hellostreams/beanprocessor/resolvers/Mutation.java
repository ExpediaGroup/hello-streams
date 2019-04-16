package com.homeaway.streamplatform.hellostreams.beanprocessor.resolvers;

import com.coxautodev.graphql.tools.GraphQLMutationResolver;
import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.beanprocessor.model.BeanSupplied;
import com.homeaway.streamplatform.hellostreams.beanprocessor.service.BeanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unused")
public class Mutation implements GraphQLMutationResolver {
    private final BeanService beanService;

    public Mutation(@Autowired BeanService beanService) {
        Preconditions.checkNotNull(beanService, "beanService cannot be null");
        this.beanService = beanService;
    }

    public BeanSupplied supplyBeans(Integer numBeans) {
        return beanService.supplyBeans(numBeans);
    }
}
