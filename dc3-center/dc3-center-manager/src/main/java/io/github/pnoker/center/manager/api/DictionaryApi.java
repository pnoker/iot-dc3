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

package io.github.pnoker.center.manager.api;

import io.github.pnoker.api.center.manager.feign.DictionaryClient;
import io.github.pnoker.center.manager.service.DictionaryService;
import io.github.pnoker.common.bean.Dictionary;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.ServiceConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(ServiceConstant.Manager.DICTIONARY_URL_PREFIX)
public class DictionaryApi implements DictionaryClient {

    @Resource
    private DictionaryService dictionaryService;

    @Override
    public R<List<Dictionary>> driverDictionary(String tenantId) {
        try {
            List<Dictionary> dictionaryList = dictionaryService.driverDictionary(tenantId);
            if (null != dictionaryList) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Dictionary>> driverAttributeDictionary(String tenantId) {
        try {
            List<Dictionary> dictionaryList = dictionaryService.driverAttributeDictionary(tenantId);
            if (null != dictionaryList) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Dictionary>> pointAttributeDictionary(String tenantId) {
        try {
            List<Dictionary> dictionaryList = dictionaryService.pointAttributeDictionary(tenantId);
            if (null != dictionaryList) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Dictionary>> profileDictionary(String tenantId) {
        try {
            List<Dictionary> dictionaryList = dictionaryService.profileDictionary(tenantId);
            if (null != dictionaryList) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Dictionary>> deviceDictionary(String tenantId) {
        try {
            List<Dictionary> dictionaryList = dictionaryService.deviceDictionary(tenantId);
            if (null != dictionaryList) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Dictionary>> pointDictionary(String parent, String tenantId) {
        try {
            List<Dictionary> dictionaryList = dictionaryService.pointDictionary(parent, tenantId);
            if (null != dictionaryList) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
}
