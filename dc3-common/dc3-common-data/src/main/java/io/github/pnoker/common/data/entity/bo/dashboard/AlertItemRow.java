/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 * Licensed under the Apache License, Version 2.0.
 */

package io.github.pnoker.common.data.entity.bo.dashboard;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/** One raw event row from the paged / latest list. */
@Getter @Setter @ToString
public class AlertItemRow {
    private long id;
    private String source;
    private long sourceId;
    private long pointId;
    private int eventTypeFlag;
    private int confirmFlag;
    private LocalDateTime createTime;
    private String message;
}
