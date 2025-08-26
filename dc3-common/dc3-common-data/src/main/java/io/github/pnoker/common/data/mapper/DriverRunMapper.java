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

package io.github.pnoker.common.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.pnoker.common.data.entity.model.DriverRunDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 驱动运行时长历史表 Mapper 接口
 * </p>
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface DriverRunMapper extends BaseMapper<DriverRunDO> {

    List<DriverRunDO> get7daysDuration(@Param("driverId") Long driverId, @Param("status") String status);

    Long selectSumDuration(@Param("driverId") Long driverId, @Param("status") String status);
}
