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

package io.github.pnoker.center.auth.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.entity.bo.TenantBO;
import io.github.pnoker.center.auth.entity.query.TenantBOPageQuery;
import io.github.pnoker.center.auth.service.TenantService;
import io.github.pnoker.common.base.Controller;
import io.github.pnoker.common.constant.enums.ResponseEnum;
import io.github.pnoker.common.constant.service.AuthServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

/**
 * 用户 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(AuthServiceConstant.TENANT_URL_PREFIX)
public class TenantController implements Controller {

    @Resource
    private TenantService tenantService;

    /**
     * 新增租户
     *
     * @param tenantBO 租户
     * @return {@link TenantBO}
     */
    @PostMapping("/add")
    public R<String> add(@Validated(Add.class) @RequestBody TenantBO tenantBO) {
        try {
            tenantService.save(tenantBO);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除租户
     *
     * @param id 租户ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") String id) {
        try {
            tenantService.remove(Long.parseLong(id));
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 更新租户
     * <ol>
     * <li>支持更新: Enable</li>
     * <li>不支持更新: Name</li>
     * </ol>
     *
     * @param tenantBO Tenant
     * @return {@link TenantBO}
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody TenantBO tenantBO) {
        try {
            tenantBO.setTenantName(null);
            tenantService.update(tenantBO);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询租户
     *
     * @param id 租户ID
     * @return {@link TenantBO}
     */
    @GetMapping("/id/{id}")
    public R<TenantBO> selectById(@NotNull @PathVariable(value = "id") String id) {
        try {
            TenantBO select = tenantService.selectById(Long.parseLong(id));
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail(ResponseEnum.NO_RESOURCE.getMessage());
    }

    /**
     * 根据 Code 查询租户
     *
     * @param code 租户Code
     * @return {@link TenantBO}
     */
    @GetMapping("/code/{code}")
    public R<TenantBO> selectByCode(@NotNull @PathVariable(value = "code") String code) {
        try {
            TenantBO select = tenantService.selectByCode(code);
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail(ResponseEnum.NO_RESOURCE.getMessage());
    }

    /**
     * 分页查询租户
     *
     * @param tenantPageQuery 租户和分页参数
     * @return 带分页的 {@link TenantBO}
     */
    @PostMapping("/list")
    public R<Page<TenantBO>> list(@RequestBody(required = false) TenantBOPageQuery tenantPageQuery) {
        try {
            if (ObjectUtil.isEmpty(tenantPageQuery)) {
                tenantPageQuery = new TenantBOPageQuery();
            }
            Page<TenantBO> page = tenantService.selectByPage(tenantPageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail(ResponseEnum.NO_RESOURCE.getMessage());
    }

}
