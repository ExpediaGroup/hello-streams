package com.homeaway.streamplatform.hellostreams.usersessionprocessor.resolvers;

import com.coxautodev.graphql.tools.GraphQLMutationResolver;
import com.google.common.base.Preconditions;
import com.homeaway.streamplatform.hellostreams.usersessionprocessor.UserSessionProcessorUtils;
import com.homeaway.streamplatform.hellostreams.usersessionprocessor.model.LoginAccepted;
import com.homeaway.streamplatform.hellostreams.usersessionprocessor.model.LoginDeclined;
import com.homeaway.streamplatform.hellostreams.usersessionprocessor.model.LoginRequested;
import com.homeaway.streamplatform.hellostreams.usersessionprocessor.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.UUID;

@Component
@SuppressWarnings("unused")
public class Mutation implements GraphQLMutationResolver {
    private final CustomerService customerService;

    public Mutation(@Autowired CustomerService customerService) {
        Preconditions.checkNotNull(customerService, "customerService cannot be null");
        this.customerService = customerService;
    }

    public LoginRequested loginRequested(String username) {
        UUID uuid = UUID.randomUUID();
        String id = uuid.toString();
        ZonedDateTime occurredAt = ZonedDateTime.now(UserSessionProcessorUtils.UTC_ZONE_ID);
        LoginRequested e = new LoginRequested();
        e.setUsername(username);
        e.setCreated(occurredAt);
        e.setId(id);
        return e;
    }

    public LoginAccepted loginAccepted(String username) {
        UUID uuid = UUID.randomUUID();
        String id = uuid.toString();
        ZonedDateTime occurredAt = ZonedDateTime.now(UserSessionProcessorUtils.UTC_ZONE_ID);
        LoginAccepted e = new LoginAccepted();
        e.setUsername(username);
        e.setCreated(occurredAt);
        e.setId(id);
        return e;
    }

    public LoginDeclined loginDeclined(String username) {
        UUID uuid = UUID.randomUUID();
        String id = uuid.toString();
        ZonedDateTime occurredAt = ZonedDateTime.now(UserSessionProcessorUtils.UTC_ZONE_ID);
        LoginDeclined e = new LoginDeclined();
        e.setCreated(occurredAt);
        e.setId(id);
        return e;
    }
}