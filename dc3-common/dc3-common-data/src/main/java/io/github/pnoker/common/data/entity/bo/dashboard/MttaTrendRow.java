/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 * Licensed under the Apache License, Version 2.0.
 */

package io.github.pnoker.common.data.entity.bo.dashboard;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** Per-day ack-latency percentile row. */
@Getter @Setter @ToString
public class MttaTrendRow {
    private String date;
    private long p50Ms;
    private long p95Ms;
    private long confirmedCount;
}
