/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 * Licensed under the Apache License, Version 2.0.
 */

package io.github.pnoker.common.data.entity.bo.dashboard;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/** Live-feed pv row — all values coerced to text so the union over 7 typed hypertables stays compatible. */
@Getter @Setter @ToString
public class LatestPointValueRow {
    private long tenantId;
    private long deviceId;
    private long pointId;
    private long driverId;
    private LocalDateTime createTime;
    private String rawValue;
    private String calValue;
    private String valueType;
}
