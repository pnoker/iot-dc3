/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.data.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DriverEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * MongoDB Object ID
     */
    private Long id;

    /**
     * 驱动服务名称
     */
    private String serviceName;

    /**
     * Driver Event
     * <p>
     * STATUS, ERROR
     */
    private String type;

    private Boolean confirm = false;
    private Object content;

    @Transient
    private int timeOut = 15;

    @Transient
    private TimeUnit timeUnit = TimeUnit.MINUTES;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

    /**
     * 确认时间
     */
    private LocalDateTime confirmTime;
}
