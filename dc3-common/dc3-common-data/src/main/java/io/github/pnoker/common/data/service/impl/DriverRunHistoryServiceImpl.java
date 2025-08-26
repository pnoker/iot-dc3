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
import io.github.pnoker.common.data.entity.model.DriverRunDO;
import io.github.pnoker.common.data.entity.model.DriverRunHistoryDO;
import io.github.pnoker.common.data.mapper.DriverRunHistoryMapper;
import io.github.pnoker.common.data.service.DriverRunHistoryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DriverRunHistoryServiceImpl extends ServiceImpl<DriverRunHistoryMapper, DriverRunHistoryDO> implements DriverRunHistoryService {
    @Resource
    private DriverRunHistoryMapper driverRunHistoryMapper;

    @Override
    public DriverRunDO getDurationDay(Long id, String code, LocalDateTime startOfDay, LocalDateTime endOfDay) {
        return driverRunHistoryMapper.getDurationDay(id, code, startOfDay, endOfDay);
    }
}
