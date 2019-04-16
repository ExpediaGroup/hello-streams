package com.homeaway.streamplatform.hellostreams.ordercleaner.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.ZonedDateTime;

@Data
@EqualsAndHashCode(callSuper=true)
public class Order extends OrderEvent {
    private String item;
    private String state;
    private String customerId;
    private ZonedDateTime updated;
}
