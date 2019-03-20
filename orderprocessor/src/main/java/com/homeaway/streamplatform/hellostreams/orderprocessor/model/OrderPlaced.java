package com.homeaway.streamplatform.hellostreams.orderprocessor.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderPlaced extends OrderEvent {
    private String customerId;
    private String item;
}
