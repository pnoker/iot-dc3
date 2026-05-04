/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 * Licensed under the Apache License, Version 2.0.
 */

package io.github.pnoker.common.data.entity.bo.dashboard;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Co-occurring event pair.
 */
@Getter
@Setter
@ToString
public class CorrelationPairRow {
    private String aSource;
    private long aSourceId;
    private int aEventType;
    private String bSource;
    private long bSourceId;
    private int bEventType;
    private long coCount;
}
