/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.api.center.auth.fallback;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.auth.feign.TenantClient;
import com.dc3.common.bean.R;
import com.dc3.common.dto.TenantDto;
import com.dc3.common.model.Tenant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * TenantClientFallback
 *
 * @author pnoker
 */
@Slf4j
@Component
public class TenantClientFallback implements FallbackFactory<TenantClient> {

    @Override
    public TenantClient create(Throwable throwable) {
        String message = throwable.getMessage() == null ? "No available server for client: DC3-CENTER-AUTH" : throwable.getMessage();
        log.error("Fallback:{}", message);

        return new TenantClient() {

            @Override
            public R<Tenant> add(Tenant user) {
                return R.fail(message);
            }

            @Override
            public R<Boolean> delete(String id) {
                return R.fail(message);
            }

            @Override
            public R<Tenant> update(Tenant user) {
                return R.fail(message);
            }

            @Override
            public R<Tenant> selectById(String id) {
                return R.fail(message);
            }

            @Override
            public R<Tenant> selectByName(String name) {
                return R.fail(message);
            }

            @Override
            public R<Page<Tenant>> list(TenantDto userDto) {
                return R.fail(message);
            }
        };
    }
}