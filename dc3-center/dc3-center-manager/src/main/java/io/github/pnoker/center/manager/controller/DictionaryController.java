/*
 * Copyright 2016-present the original author or authors.
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

package io.github.pnoker.center.manager.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.DictionaryPageQuery;
import io.github.pnoker.center.manager.service.DictionaryService;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.Dictionary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 字典 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerServiceConstant.DICTIONARY_URL_PREFIX)
public class DictionaryController {

    @Resource
    private DictionaryService dictionaryService;

    /**
     * 查询驱动 Dictionary
     *
     * @param dictionaryPageQuery DictionaryDto
     * @param tenantId            租户ID
     * @return Page Of Dictionary
     */
    @PostMapping("/driver")
    public R<Page<Dictionary>> driverDictionary(@RequestBody(required = false) DictionaryPageQuery dictionaryPageQuery,
                                                @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            if (ObjectUtil.isEmpty(dictionaryPageQuery)) {
                dictionaryPageQuery = new DictionaryPageQuery();
            }
            dictionaryPageQuery.setTenantId(tenantId);
            Page<Dictionary> page = dictionaryService.driverDictionary(dictionaryPageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 查询模板 Dictionary
     *
     * @param dictionaryPageQuery DictionaryDto
     * @param tenantId            租户ID
     * @return Page Of Dictionary
     */
    @PostMapping("/profile")
    public R<Page<Dictionary>> profileDictionary(@RequestBody(required = false) DictionaryPageQuery dictionaryPageQuery,
                                                 @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            if (ObjectUtil.isEmpty(dictionaryPageQuery)) {
                dictionaryPageQuery = new DictionaryPageQuery();
            }
            dictionaryPageQuery.setTenantId(tenantId);
            Page<Dictionary> page = dictionaryService.profileDictionary(dictionaryPageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 查询设备 Dictionary
     *
     * @param dictionaryPageQuery DictionaryDto
     * @param tenantId            租户ID
     * @return Page Of Dictionary
     */
    @PostMapping("/device")
    public R<Page<Dictionary>> deviceDictionary(@RequestBody(required = false) DictionaryPageQuery dictionaryPageQuery,
                                                @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            if (ObjectUtil.isEmpty(dictionaryPageQuery)) {
                dictionaryPageQuery = new DictionaryPageQuery();
            }
            dictionaryPageQuery.setTenantId(tenantId);
            Page<Dictionary> page = dictionaryService.deviceDictionary(dictionaryPageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 查询位号 Dictionary
     *
     * @param dictionaryPageQuery DictionaryDto
     * @param tenantId            租户ID
     * @return Page Of Dictionary
     */
    @PostMapping("/point")
    public R<Page<Dictionary>> pointDictionary(@RequestBody(required = false) DictionaryPageQuery dictionaryPageQuery,
                                               @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            if (ObjectUtil.isEmpty(dictionaryPageQuery)) {
                dictionaryPageQuery = new DictionaryPageQuery();
            }
            dictionaryPageQuery.setTenantId(tenantId);
            Page<Dictionary> page = dictionaryService.pointDictionary(dictionaryPageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
}
