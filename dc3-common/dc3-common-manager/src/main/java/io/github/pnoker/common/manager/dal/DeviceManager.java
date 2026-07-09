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
 * Persistence manager for device CRUD operations.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface DeviceManager extends IService<DeviceDO> {

    /**
     * Check whether a device is duplicated by device name, device code (when present),
     * and tenant. Unlike the throwing variant, this only reports the duplicate without
     * raising an exception.
     *
     * @param entityDO {@link DeviceDO} to be validated
     * @param isUpdate whether the operation is an update (true) or create (false)
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    boolean checkDuplicate(DeviceDO entityDO, boolean isUpdate);

    /**
     * Create a device after a duplicate check, throwing {@code DuplicateException} on
     * conflict. Returns the persisted record on success.
     *
     * @param entityDO {@link DeviceDO} to create
     * @return the persisted {@link DeviceDO}
     */
    DeviceDO innerSave(DeviceDO entityDO);

}
