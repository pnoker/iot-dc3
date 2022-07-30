/*
 * Copyright 2022 Pnoker All Rights Reserved
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

package io.github.pnoker.center.auth.service;

import io.github.pnoker.common.base.Service;
import io.github.pnoker.common.dto.TenantBindDto;
import io.github.pnoker.common.model.TenantBind;

/**
 * TenantBind Interface
 *
 * @author pnoker
 */
public interface TenantBindService extends Service<TenantBind, TenantBindDto> {

    /**
     * 根据 租户ID 和 关联的用户ID 查询
     *
     * @param tenantId TenantID
     * @param userId   userId
     * @return TenantBind
     */
    TenantBind selectByTenantIdAndUserId(String tenantId, String userId);
}
