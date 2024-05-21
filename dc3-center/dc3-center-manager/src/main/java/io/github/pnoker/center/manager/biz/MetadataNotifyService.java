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

package io.github.pnoker.center.manager.biz;

import io.github.pnoker.center.manager.entity.bo.DeviceBO;
import io.github.pnoker.center.manager.entity.bo.PointBO;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;

/**
 * 元数据通知接口
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface MetadataNotifyService {

    /**
     * 通知驱动 新增设备(ADD) / 删除设备(DELETE) / 更新设备(UPDATE)
     *
     * @param operate  Operation Type
     * @param metadata Device
     */
    void notifyDevice(MetadataOperateTypeEnum operate, DeviceBO metadata);

    /**
     * 通知驱动 新增位号(ADD) / 删除位号(DELETE) / 更新位号(UPDATE)
     *
     * @param operate  Operation Type
     * @param metadata Point
     */
    void notifyPoint(MetadataOperateTypeEnum operate, PointBO metadata);

}
