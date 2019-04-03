package com.homeaway.streamplatform.hellostreams.orderprocessor.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.ZonedDateTime;

@Data
@EqualsAndHashCode(callSuper=true)
public class Customer extends CustomerEvent {
    private String state;
    private ZonedDateTime created;
    private ZonedDateTime updated;
}