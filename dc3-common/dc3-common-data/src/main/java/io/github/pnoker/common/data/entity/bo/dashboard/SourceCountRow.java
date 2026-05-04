/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 * Licensed under the Apache License, Version 2.0.
 */

package io.github.pnoker.common.data.entity.bo.dashboard;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * (source, sourceId, count) — one rank entry.
 */
@Getter
@Setter
@ToString
public class SourceCountRow {
    private String source;
    private long sourceId;
    private long count;
}
