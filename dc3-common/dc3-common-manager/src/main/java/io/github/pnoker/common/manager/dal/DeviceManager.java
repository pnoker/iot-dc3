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

package io.github.pnoker.common.manager.dal;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.model.DeviceDO;

/**
 * <p>
 * 设备表 服务类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface DeviceManager extends IService<DeviceDO> {

    /**
     * 重复性校验
     *
     * @param entityDO {@link DeviceDO}
     * @param isUpdate 是否为更新操作
     * @return 是否重复
     */
    boolean checkDuplicate(DeviceDO entityDO, boolean isUpdate);

    /**
     * 内部保存
     *
     * @param entityDO {@link DeviceBO}
     * @return {@link DeviceDO}
     */
    DeviceDO innerSave(DeviceDO entityDO);

}
