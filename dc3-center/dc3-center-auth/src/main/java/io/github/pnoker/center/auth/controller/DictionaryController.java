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
import io.github.pnoker.center.auth.biz.DictionaryService;
import io.github.pnoker.common.base.Controller;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.bo.DictionaryBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping(AuthConstant.DICTIONARY_URL_PREFIX)
public class DictionaryController implements Controller {

    @Resource
    private DictionaryService dictionaryService;

    /**
     * 查询租户字典列表
     *
     * @return 字典列表
     */
    @GetMapping("/tenant")
    public R<List<DictionaryBO>> tenantDictionary() {
        try {
            List<DictionaryBO> dictionaryList = dictionaryService.tenantDictionary();
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
     * @return 字典列表
     */
    @GetMapping("/user")
    public R<List<DictionaryBO>> userDictionary() {
        try {
            List<DictionaryBO> dictionaryList = dictionaryService.userLoginDictionary(getTenantId());
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
     * @return 字典列表
     */
    @GetMapping("/limited_ip")
    public R<List<DictionaryBO>> limitedIpDictionary() {
        try {
            List<DictionaryBO> dictionaryList = dictionaryService.limitedIpDictionary(getTenantId());
            if (ObjectUtil.isNotNull(dictionaryList)) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
