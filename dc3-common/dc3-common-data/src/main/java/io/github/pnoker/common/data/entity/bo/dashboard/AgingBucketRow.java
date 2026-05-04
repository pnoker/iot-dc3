/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 * Licensed under the Apache License, Version 2.0.
 */

package io.github.pnoker.common.data.entity.bo.dashboard;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Four-age-bucket counts + total unconfirmed.
 */
@Getter
@Setter
@ToString
public class AgingBucketRow {
    private long under1h;
    private long h1to6;
    private long h6to24;
    private long over24h;
    private long total;
}
