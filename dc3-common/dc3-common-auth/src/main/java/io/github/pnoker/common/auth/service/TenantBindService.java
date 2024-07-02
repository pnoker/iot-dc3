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

package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.TenantBindBO;
import io.github.pnoker.common.auth.entity.query.TenantBindQuery;
import io.github.pnoker.common.base.service.BaseService;

/**
 * TenantBind Interface
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface TenantBindService extends BaseService<TenantBindBO, TenantBindQuery> {

    /**
     * 根据 租户ID 和 关联的用户ID 查询
     *
     * @param tenantId 租户ID
     * @param userId   User ID
     * @return TenantBind
     */
    TenantBindBO selectByTenantIdAndUserId(Long tenantId, Long userId);
}
