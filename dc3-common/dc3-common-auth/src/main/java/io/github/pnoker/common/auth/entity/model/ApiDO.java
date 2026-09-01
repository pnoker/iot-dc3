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

package io.github.pnoker.common.auth.entity.model;

import io.github.pnoker.common.entity.ext.JsonExt;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * Persistence object for the dc3_api table.
 *
 * </p>
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Getter
@Setter
@ToString
public class ApiDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Primary key ID
     */
    private Long id;

    /**
     * Owning service name, populated by resource registrar
     */
    private String serviceName;

    /**
     * Type
     */
    private Byte apiTypeFlag;

    /**
     * Name
     */
    private String apiName;

    /**
     * Code
     */
    private String apiCode;

    /**
     * API grouping, usually the owning controller simple class name
     */
    private String apiGroup;

    /**
     *
     */
    private JsonExt apiExt;

    /**
     * Enable flag, 0:Enable, 1:Disable
     */
    private Byte enableFlag;

    /**
     * Description
     */
    private String remark;

    /**
     * Creator ID
     */
    private Long creatorId;

    /**
     * Creator Name
     */
    private String creatorName;

    /**
     * Create Time
     */
    private LocalDateTime createTime;

    /**
     * Operator ID
     */
    private Long operatorId;

    /**
     * Operator Name
     */
    private String operatorName;

    /**
     * Operate Time
     */
    private LocalDateTime operateTime;

    /**
     * Logical delete flag, 0:not deleted, 1:deleted
     */
    private Byte deleted;

}
