package com.homeaway.streamplatform.hellostreams.usersessionprocessor.model;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class CommandEvent {
    private String id;
    private ZonedDateTime created;
}
