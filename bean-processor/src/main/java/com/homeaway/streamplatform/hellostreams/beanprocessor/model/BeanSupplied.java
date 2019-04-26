package com.homeaway.streamplatform.hellostreams.beanprocessor.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class BeanSupplied extends CommandEvent {
   private int beansSupplied;
}
