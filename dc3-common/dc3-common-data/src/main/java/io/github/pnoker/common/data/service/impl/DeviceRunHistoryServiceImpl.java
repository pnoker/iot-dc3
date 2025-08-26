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

package io.github.pnoker.common.data.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.common.data.entity.model.DeviceRunDO;
import io.github.pnoker.common.data.entity.model.DeviceRunHistoryDO;
import io.github.pnoker.common.data.mapper.DeviceRunHistoryMapper;
import io.github.pnoker.common.data.service.DeviceRunHistoryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DeviceRunHistoryServiceImpl extends ServiceImpl<DeviceRunHistoryMapper, DeviceRunHistoryDO> implements DeviceRunHistoryService {
    @Resource
    private DeviceRunHistoryMapper deviceRunHistoryMapper;

    @Override
    public DeviceRunDO getDurationDay(Long id, String code, LocalDateTime startOfDay, LocalDateTime endOfDay) {
        return deviceRunHistoryMapper.getDurationDay(id, code, startOfDay, endOfDay);
    }
}
