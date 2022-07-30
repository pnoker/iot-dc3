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

package io.github.pnoker.api.center.auth.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.auth.fallback.TenantClientFallback;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.ServiceConstant;
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
 */
@FeignClient(path = ServiceConstant.Auth.TENANT_URL_PREFIX, name = ServiceConstant.Auth.SERVICE_NAME, fallbackFactory = TenantClientFallback.class)
public interface TenantClient {

    /**
     * 新增 Tenant
     *
     * @param tenant Tenant
     * @return Tenant
     */
    @PostMapping("/add")
    R<Tenant> add(@Validated(Insert.class) @RequestBody Tenant tenant);

    /**
     * 根据 ID 删除 Tenant
     *
     * @param id Tenant Id
     * @return Boolean
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@NotNull @PathVariable(value = "id") String id);

    /**
     * 修改 Tenant
     * <p>
     * 支  持: Enable
     * 不支持: Name
     *
     * @param tenant Tenant
     * @return Tenant
     */
    @PostMapping("/update")
    R<Tenant> update(@Validated(Update.class) @RequestBody Tenant tenant);

    /**
     * 根据 ID 查询 Tenant
     *
     * @param id Tenant Id
     * @return Tenant
     */
    @GetMapping("/id/{id}")
    R<Tenant> selectById(@NotNull @PathVariable(value = "id") String id);

    /**
     * 根据 Name 查询 Tenant
     *
     * @param name Tenant Name
     * @return Tenant
     */
    @GetMapping("/name/{name}")
    R<Tenant> selectByName(@NotNull @PathVariable(value = "name") String name);

    /**
     * 分页查询 Tenant
     *
     * @param tenantDto Dto
     * @return Page<Tenant>
     */
    @PostMapping("/list")
    R<Page<Tenant>> list(@RequestBody(required = false) TenantDto tenantDto);

}
