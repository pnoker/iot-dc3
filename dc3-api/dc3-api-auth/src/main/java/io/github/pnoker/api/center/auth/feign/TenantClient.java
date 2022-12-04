/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.api.center.auth.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.auth.fallback.TenantClientFallback;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.service.AuthServiceConstant;
import io.github.pnoker.common.dto.TenantDto;
import io.github.pnoker.common.model.Tenant;
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.constraints.NotNull;

/**
 * 租户 FeignClient
 *
 * @author pnoker
 * @since 2022.1.0
 */
@FeignClient(path = AuthServiceConstant.TENANT_URL_PREFIX, name = AuthServiceConstant.SERVICE_NAME, fallbackFactory = TenantClientFallback.class)
public interface TenantClient {

    /**
     * 新增租户
     *
     * @param tenant 租户
     * @return {@link io.github.pnoker.common.model.Tenant}
     */
    @PostMapping("/add")
    R<Tenant> add(@Validated(Insert.class) @RequestBody Tenant tenant);

    /**
     * 根据 ID 删除租户
     *
     * @param id 租户ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@NotNull @PathVariable(value = "id") String id);

    /**
     * 根据 ID 修改租户
     * <ol>
     * <li>支持修改: Enable</li>
     * <li>不支持修改: Name</li>
     * </ol>
     *
     * @param tenant Tenant
     * @return {@link io.github.pnoker.common.model.Tenant}
     */
    @PostMapping("/update")
    R<Tenant> update(@Validated(Update.class) @RequestBody Tenant tenant);

    /**
     * 根据 ID 查询租户
     *
     * @param id 租户ID
     * @return {@link io.github.pnoker.common.model.Tenant}
     */
    @GetMapping("/id/{id}")
    R<Tenant> selectById(@NotNull @PathVariable(value = "id") String id);

    /**
     * 根据 Name 查询租户
     *
     * @param name 租户名称
     * @return {@link io.github.pnoker.common.model.Tenant}
     */
    @GetMapping("/name/{name}")
    R<Tenant> selectByName(@NotNull @PathVariable(value = "name") String name);

    /**
     * 模糊分页查询租户
     *
     * @param tenantDto 租户和分页参数
     * @return 带分页的 {@link io.github.pnoker.common.model.Tenant}
     */
    @PostMapping("/list")
    R<Page<Tenant>> list(@RequestBody(required = false) TenantDto tenantDto);

}
