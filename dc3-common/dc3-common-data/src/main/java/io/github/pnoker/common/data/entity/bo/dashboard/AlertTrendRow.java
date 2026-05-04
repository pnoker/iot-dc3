/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 * Licensed under the Apache License, Version 2.0.
 */

package io.github.pnoker.common.data.entity.bo.dashboard;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** One day's split alert count (device vs driver source). */
@Getter @Setter @ToString
public class AlertTrendRow {
    private String date;
    private long deviceCount;
    private long driverCount;
}
