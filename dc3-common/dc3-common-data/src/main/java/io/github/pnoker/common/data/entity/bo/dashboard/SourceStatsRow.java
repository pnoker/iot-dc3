/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 * Licensed under the Apache License, Version 2.0.
 */

package io.github.pnoker.common.data.entity.bo.dashboard;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * (source, total, unconfirmed) — per-source alert counts.
 */
@Getter
@Setter
@ToString
public class SourceStatsRow {
    private String source;
    private long total;
    private long unconfirmed;
}
