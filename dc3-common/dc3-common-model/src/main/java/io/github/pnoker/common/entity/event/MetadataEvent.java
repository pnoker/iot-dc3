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

package io.github.pnoker.common.entity.event;

import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 元数据事件
 *
 * @author zhangzi
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
