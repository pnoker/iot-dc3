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
