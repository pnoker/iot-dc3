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

package io.github.pnoker.common.driver.event.metadata;

import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 元数据事件 Publisher
 *
 * @author zhangzi
 * @since 2022.1.0
 */
@Slf4j
@Component
public class MetadataEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public MetadataEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * 发布事件
     *
     * @param metadataEvent MetadataEvent
     */
    public void publishEvent(MetadataEvent metadataEvent) {
        log.info("Metadata event publisher publishEvent: {}", JsonUtil.toJsonString(metadataEvent));
        applicationEventPublisher.publishEvent(metadataEvent);
    }
}
