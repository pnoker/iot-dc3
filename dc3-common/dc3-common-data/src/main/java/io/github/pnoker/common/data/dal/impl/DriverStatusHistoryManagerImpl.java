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

package io.github.pnoker.common.data.dal.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.common.data.dal.DriverStatusHistoryManager;
import io.github.pnoker.common.data.entity.model.DriverStatusHistoryDO;
import io.github.pnoker.common.data.mapper.DriverStatusHistoryMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 驱动状态历史表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2024-03-07
 */
@Service
public class DriverStatusHistoryManagerImpl extends ServiceImpl<DriverStatusHistoryMapper, DriverStatusHistoryDO> implements DriverStatusHistoryManager {

}
