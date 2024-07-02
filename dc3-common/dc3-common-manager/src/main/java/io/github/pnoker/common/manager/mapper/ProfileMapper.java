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

package io.github.pnoker.common.manager.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.manager.entity.model.ProfileDO;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 设备模版表 Mapper 接口
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface ProfileMapper extends BaseMapper<ProfileDO> {

    Page<ProfileDO> selectPageWithDevice(Page<ProfileDO> page, @Param(Constants.WRAPPER) Wrapper<ProfileDO> wrapper, @Param("deviceId") Long deviceId);
}
