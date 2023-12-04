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

package io.github.pnoker.center.manager.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.DictionaryQuery;
import io.github.pnoker.center.manager.service.DictionaryService;
import io.github.pnoker.common.base.Controller;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.Dictionary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
public class DictionaryController implements Controller {

    @Resource
    private DictionaryService dictionaryService;

    /**
     * 查询驱动 Dictionary
     *
     * @param dictionaryQuery DictionaryDto
     * @return Page Of Dictionary
     */
    @PostMapping("/driver")
    public R<Page<Dictionary>> driverDictionary(@RequestBody(required = false) DictionaryQuery dictionaryQuery) {
        try {
            if (ObjectUtil.isEmpty(dictionaryQuery)) {
                dictionaryQuery = new DictionaryQuery();
            }
            dictionaryQuery.setTenantId(getTenantId());
            Page<Dictionary> page = dictionaryService.driverDictionary(dictionaryQuery);
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
     * @param dictionaryQuery DictionaryDto
     * @return Page Of Dictionary
     */
    @PostMapping("/profile")
    public R<Page<Dictionary>> profileDictionary(@RequestBody(required = false) DictionaryQuery dictionaryQuery) {
        try {
            if (ObjectUtil.isEmpty(dictionaryQuery)) {
                dictionaryQuery = new DictionaryQuery();
            }
            dictionaryQuery.setTenantId(getTenantId());
            Page<Dictionary> page = dictionaryService.profileDictionary(dictionaryQuery);
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
     * @param dictionaryQuery DictionaryDto
     * @return Page Of Dictionary
     */
    @PostMapping("/device")
    public R<Page<Dictionary>> deviceDictionary(@RequestBody(required = false) DictionaryQuery dictionaryQuery) {
        try {
            if (ObjectUtil.isEmpty(dictionaryQuery)) {
                dictionaryQuery = new DictionaryQuery();
            }
            dictionaryQuery.setTenantId(getTenantId());
            Page<Dictionary> page = dictionaryService.deviceDictionary(dictionaryQuery);
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
     * @param dictionaryQuery DictionaryDto
     * @return Page Of Dictionary
     */
    @PostMapping("/point")
    public R<Page<Dictionary>> pointDictionary(@RequestBody(required = false) DictionaryQuery dictionaryQuery) {
        try {
            if (ObjectUtil.isEmpty(dictionaryQuery)) {
                dictionaryQuery = new DictionaryQuery();
            }
            dictionaryQuery.setTenantId(getTenantId());
            Page<Dictionary> page = dictionaryService.pointDictionary(dictionaryQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
}
