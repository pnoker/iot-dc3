/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 * Licensed under the Apache License, Version 2.0.
 */

package io.github.pnoker.common.data.entity.bo.dashboard;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Tenant-wide (total, unconfirmed) counters — the service layer combines
 * this with separate per-source and today-only queries to populate the
 * full AlertStatsVO.
 */
@Getter
@Setter
@ToString
public class AlertCountersRow {
    private long total;
    private long unconfirmed;
}
