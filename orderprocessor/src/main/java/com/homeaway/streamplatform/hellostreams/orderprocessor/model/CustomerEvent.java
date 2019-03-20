package com.homeaway.streamplatform.hellostreams.orderprocessor.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class CustomerEvent extends CommandEvent {
    private String username;
}
