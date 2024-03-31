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

package io.github.pnoker.center.manager.dal.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.center.manager.dal.ProduceManager;
import io.github.pnoker.center.manager.entity.model.ProduceDO;
import io.github.pnoker.center.manager.mapper.ProduceMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 主题分量表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2024-03-31
 */
@Service
public class ProduceManagerImpl extends ServiceImpl<ProduceMapper, ProduceDO> implements ProduceManager {

}
