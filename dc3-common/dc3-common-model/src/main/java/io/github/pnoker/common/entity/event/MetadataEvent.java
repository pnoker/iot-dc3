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

package io.github.pnoker.common.entity.event;

import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Metadata event.
 *
 * @author zhangzi
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
public class MetadataEvent extends ApplicationEvent {

    private final Long id;

    private final MetadataTypeEnum metadataType;

    private final MetadataOperateTypeEnum operateType;

    private final Set<String> targetServices;

    /**
     * Constructor.
     *
     * @param source       Event source object
     * @param id           Metadata ID
     * @param metadataType Metadata type
     * @param operateType  Metadata operation type
     */
    public MetadataEvent(Object source, Long id, MetadataTypeEnum metadataType, MetadataOperateTypeEnum operateType) {
        this(source, id, metadataType, operateType, Collections.emptySet());
    }

    /**
     * Constructor.
     *
     * @param source         Event source object
     * @param id             Metadata ID
     * @param metadataType   Metadata type
     * @param operateType    Metadata operation type
     * @param targetServices Driver services that must receive this event
     */
    public MetadataEvent(Object source, Long id, MetadataTypeEnum metadataType, MetadataOperateTypeEnum operateType,
                         Collection<String> targetServices) {
        super(source);
        this.id = id;
        this.metadataType = metadataType;
        this.operateType = operateType;
        this.targetServices = Objects.isNull(targetServices) ? Collections.emptySet()
                : targetServices.stream()
                .filter(Objects::nonNull)
                .filter(service -> !service.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

}
