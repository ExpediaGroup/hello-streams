package com.homeaway.streamplatform.hellostreams.ordercleaner.resolvers;

import com.coxautodev.graphql.tools.GraphQLQueryResolver;
import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.ordercleaner.OrderCleanerUtils;
import com.homeaway.streamplatform.hellostreams.ordercleaner.service.OrderCleanerTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZonedDateTime;

@SuppressWarnings("unused")
@Component
public class Query implements GraphQLQueryResolver {
    private OrderCleanerTask orderCleanerTask;

    public Query(@Autowired OrderCleanerTask orderCleanerTask) {
        Preconditions.checkNotNull(orderCleanerTask, "orderCleanerTask cannot be null");

        this.orderCleanerTask = orderCleanerTask;
    }

    public ZonedDateTime getNextCleanTime() {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(orderCleanerTask.getNextStartTime().toInstant().getMillis()), OrderCleanerUtils.UTC_ZONE_ID);
    }
}
