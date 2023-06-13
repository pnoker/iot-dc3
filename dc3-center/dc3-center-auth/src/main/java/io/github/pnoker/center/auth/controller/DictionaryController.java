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

package io.github.pnoker.center.auth.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.center.auth.service.DictionaryService;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.AuthServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.Dictionary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 字典 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(AuthServiceConstant.DICTIONARY_URL_PREFIX)
public class DictionaryController {

    @Resource
    private DictionaryService dictionaryService;

    /**
     * 查询租户字典列表
     *
     * @return 字典列表
     */
    @GetMapping("/tenant")
    public R<List<Dictionary>> tenantDictionary() {
        try {
            List<Dictionary> dictionaryList = dictionaryService.tenantDictionary();
            if (ObjectUtil.isNotNull(dictionaryList)) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 查询用户字典列表
     *
     * @param tenantId 租户ID
     * @return 字典列表
     */
    @GetMapping("/user")
    public R<List<Dictionary>> userDictionary(@RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            List<Dictionary> dictionaryList = dictionaryService.userDictionary(tenantId);
            if (ObjectUtil.isNotNull(dictionaryList)) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 查询 Ip 黑名单字典列表
     *
     * @param tenantId 租户ID
     * @return 字典列表
     */
    @GetMapping("/black_ip")
    public R<List<Dictionary>> blackIpDictionary(@RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            List<Dictionary> dictionaryList = dictionaryService.blackIpDictionary(tenantId);
            if (ObjectUtil.isNotNull(dictionaryList)) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
