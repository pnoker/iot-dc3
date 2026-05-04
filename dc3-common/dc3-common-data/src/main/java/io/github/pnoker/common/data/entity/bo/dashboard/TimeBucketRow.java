/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 * Licensed under the Apache License, Version 2.0.
 */

package io.github.pnoker.common.data.entity.bo.dashboard;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * One time-bucketed count row. Bucket comes from Postgres {@code time_bucket()}
 * which returns TIMESTAMPTZ; JDBC maps it to LocalDateTime directly so no
 * service-side parsing is needed.
 */
@Getter
@Setter
@ToString
public class TimeBucketRow {
    private LocalDateTime bucket;
    private long count;
}
