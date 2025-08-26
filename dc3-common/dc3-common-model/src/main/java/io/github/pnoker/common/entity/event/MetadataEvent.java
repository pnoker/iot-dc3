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

/**
 * 元数据事件
 *
 * @author zhangzi
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Getter
public class MetadataEvent extends ApplicationEvent {

    private final Long id;
    private final MetadataTypeEnum metadataType;
    private final MetadataOperateTypeEnum operateType;

    /**
     * 构造函数
     *
     * @param source       Object
     * @param id           ID
     * @param metadataType 元数据类型
     * @param operateType  元数据操作类型
     */
    public MetadataEvent(Object source,
                         Long id,
                         MetadataTypeEnum metadataType,
                         MetadataOperateTypeEnum operateType) {
        super(source);
        this.id = id;
        this.metadataType = metadataType;
        this.operateType = operateType;
    }
}
