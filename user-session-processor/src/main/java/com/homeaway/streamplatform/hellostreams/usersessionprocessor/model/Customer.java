package com.homeaway.streamplatform.hellostreams.usersessionprocessor.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.ZonedDateTime;

@Data
@EqualsAndHashCode(callSuper=true)
public class Customer extends CustomerEvent {
    private String username;
    private String state;
    private ZonedDateTime created;
    private ZonedDateTime updated;
}