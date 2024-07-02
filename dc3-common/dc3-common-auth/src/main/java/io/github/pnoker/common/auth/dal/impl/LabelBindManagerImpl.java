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

package io.github.pnoker.common.auth.dal.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.common.auth.dal.LabelBindManager;
import io.github.pnoker.common.auth.entity.model.LabelBindDO;
import io.github.pnoker.common.auth.mapper.LabelBindMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 标签关联表 服务实现类
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Service
public class LabelBindManagerImpl extends ServiceImpl<LabelBindMapper, LabelBindDO> implements LabelBindManager {

}
