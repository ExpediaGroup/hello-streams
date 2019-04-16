package com.homeaway.streamplatform.hellostreams.ordercleaner.model;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class CommandEvent {
    private String id;
    private ZonedDateTime created;
}
