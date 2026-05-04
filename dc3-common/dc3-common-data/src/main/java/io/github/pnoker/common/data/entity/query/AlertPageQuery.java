/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.data.entity.query;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * Request body for {@code POST /dashboard/alert/page}. Replaces the
 * earlier Map&lt;String, Object&gt; shape so fields are typed and the
 * controller doesn't have to handwrite {@code Integer.parseInt(b.get("..").toString())}
 * for every entry.
 *
 * @author pnoker
 * @since 2026.5.4
 */
@Getter
@Setter
@ToString
public class AlertPageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * {@code "device"} / {@code "driver"} / {@code null} (both).
     */
    private String source;

    private Integer eventTypeFlag;

    /**
     * 0 = unconfirmed, 1 = confirmed, null = both.
     */
    private Integer confirmFlag;

    /**
     * Legacy integer window; {@code rangeKey} wins when both set.
     */
    private Integer rangeHours;

    /**
     * Preset time-range key — resolved server-side via TimeRangeUtil.
     */
    private String rangeKey;

    /**
     * 1-based page index. Defaults to 1 if null or less.
     */
    private Long current;

    private Long size;
}
