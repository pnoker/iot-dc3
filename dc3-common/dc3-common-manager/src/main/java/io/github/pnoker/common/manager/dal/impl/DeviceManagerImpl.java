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

package io.github.pnoker.common.manager.dal.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.manager.dal.DeviceManager;
import io.github.pnoker.common.manager.entity.model.DeviceDO;
import io.github.pnoker.common.manager.mapper.DeviceMapper;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * <p>
 * 设备表 服务实现类
 * </p>
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Service
public class DeviceManagerImpl extends ServiceImpl<DeviceMapper, DeviceDO> implements DeviceManager {

    @Override
    public boolean checkDuplicate(DeviceDO entityDO, boolean isUpdate) {
        LambdaQueryWrapper<DeviceDO> wrapper = Wrappers.<DeviceDO>query().lambda();
        wrapper.eq(DeviceDO::getDeviceName, entityDO.getDeviceName());
        wrapper.eq(CharSequenceUtil.isNotEmpty(entityDO.getDeviceCode()), DeviceDO::getDeviceCode, entityDO.getDeviceCode());
        wrapper.eq(DeviceDO::getTenantId, entityDO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        DeviceDO one = getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        return !isUpdate || !one.getId().equals(entityDO.getId());
    }

    @Override
    public DeviceDO innerSave(DeviceDO entityDO) {
        boolean duplicate = checkDuplicate(entityDO, false);
        if (duplicate) {
            throw new DuplicateException("Failed to create device: device has been duplicated");
        }

        if (!save(entityDO)) {
            throw new AddException("Failed to create device");
        }
        return entityDO;
    }


}
