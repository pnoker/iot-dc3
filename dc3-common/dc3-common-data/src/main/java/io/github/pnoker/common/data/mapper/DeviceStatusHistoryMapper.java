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

package io.github.pnoker.common.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.pnoker.common.data.entity.model.DeviceStatusHistoryDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 设备状态历史表 Mapper 接口
 * </p>
 *
 * @author pnoker
 * @since 2024-03-07
 */
public interface DeviceStatusHistoryMapper extends BaseMapper<DeviceStatusHistoryDO> {

    List<DeviceStatusHistoryDO> selectRecently2Data(@Param("id") long id);
}
