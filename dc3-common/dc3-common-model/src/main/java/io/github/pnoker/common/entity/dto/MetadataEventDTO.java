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

package io.github.pnoker.common.entity.dto;

import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * Data transfer object for metadata change events propagated via RabbitMQ.
 *
 * @author zhangzi
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
public class MetadataEventDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Type{@link MetadataTypeEnum}, ,
     */
    private Long id;

    private Long tenantId;

    /**
     * Type
     */
    private MetadataTypeEnum metadataType;

    /**
     * Type, , ,
     */
    private MetadataOperateTypeEnum operateType;

    public MetadataEventDTO(Long id, MetadataTypeEnum metadataType, MetadataOperateTypeEnum operateType) {
        this(null, id, metadataType, operateType);
    }

    public MetadataEventDTO(Long tenantId, Long id, MetadataTypeEnum metadataType,
                            MetadataOperateTypeEnum operateType) {
        this.tenantId = tenantId;
        this.id = id;
        this.metadataType = metadataType;
        this.operateType = operateType;
    }

}
