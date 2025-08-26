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
 * @version 2025.6.0
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
