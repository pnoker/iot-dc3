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

package io.github.pnoker.center.manager.event.notify;

import io.github.pnoker.center.manager.biz.MetadataNotifyService;
import io.github.pnoker.center.manager.entity.bo.DeviceBO;
import io.github.pnoker.center.manager.entity.bo.PointBO;
import io.github.pnoker.common.entity.base.BaseBO;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 元数据事件 Listener
 *
 * @author zhangzi
 * @since 2022.1.0
 */
@Slf4j
@Component
public class MetadataEventListener implements ApplicationListener<MetadataEvent<? extends BaseBO>> {

    private final MetadataNotifyService metadataNotifyService;

    public MetadataEventListener(MetadataNotifyService metadataNotifyService) {
        this.metadataNotifyService = metadataNotifyService;
    }

    @Override
    public void onApplicationEvent(@NotNull MetadataEvent metadataEvent) {
        log.info("Metadata event listener received: {}", JsonUtil.toJsonString(metadataEvent));
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        switch (metadataType) {
            case DRIVER -> {
                // to do something for driver event
            }
            case DEVICE -> {
                MetadataOperateTypeEnum operate = metadataEvent.getOperateType();
                DeviceBO metadata = (DeviceBO) metadataEvent.getMetadata();
                metadataNotifyService.notifyDevice(operate, metadata);
            }
            case POINT -> {
                MetadataOperateTypeEnum operate = metadataEvent.getOperateType();
                PointBO metadata = (PointBO) metadataEvent.getMetadata();
                metadataNotifyService.notifyPoint(operate, metadata);
            }
            default -> log.warn("There is no event of this metadata: {}", metadataEvent);
        }

    }
}
