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

package com.dc3.center.auth.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.auth.feign.TenantClient;
import com.dc3.center.auth.service.TenantService;
import com.dc3.common.bean.R;
import com.dc3.common.constant.ServiceConstant;
import com.dc3.common.dto.TenantDto;
import com.dc3.common.model.Tenant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 用户 Feign Client 接口实现
 *
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(ServiceConstant.Auth.TENANT_URL_PREFIX)
public class TenantApi implements TenantClient {
    @Resource
    private TenantService tenantService;

    @Override
    public R<Tenant> add(Tenant tenant) {
        try {
            Tenant add = tenantService.add(tenant);
            if (null != add) {
                return R.ok(add);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Boolean> delete(String id) {
        try {
            return tenantService.delete(id) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<Tenant> update(Tenant tenant) {
        try {
            Tenant update = tenantService.update(tenant.setName(null));
            if (null != update) {
                return R.ok(update);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Tenant> selectById(String id) {
        try {
            Tenant select = tenantService.selectById(id);
            if (null != select) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail("Resource does not exist");
    }

    @Override
    public R<Tenant> selectByName(String name) {
        try {
            Tenant select = tenantService.selectByName(name);
            if (null != select) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail("Resource does not exist");
    }

    @Override
    public R<Page<Tenant>> list(TenantDto tenantDto) {
        try {
            Page<Tenant> page = tenantService.list(tenantDto);
            if (null != page) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail("Resource does not exist");
    }

}
