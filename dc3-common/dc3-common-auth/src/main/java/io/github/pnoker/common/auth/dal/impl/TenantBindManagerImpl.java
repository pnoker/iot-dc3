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

package io.github.pnoker.common.auth.dal.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.pnoker.common.auth.dal.TenantBindManager;
import io.github.pnoker.common.auth.entity.model.TenantBindDO;
import io.github.pnoker.common.auth.mapper.TenantBindMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 租户和用户的绑定表 服务实现类
 * </p>
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Service
public class TenantBindManagerImpl extends ServiceImpl<TenantBindMapper, TenantBindDO> implements TenantBindManager {

}
