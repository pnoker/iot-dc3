/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.manager.api;

import com.dc3.api.center.manager.feign.DictionaryClient;
import com.dc3.center.manager.service.DictionaryService;
import com.dc3.common.bean.Dictionary;
import com.dc3.common.bean.R;
import com.dc3.common.constant.Common;
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
@RequestMapping(Common.Service.DC3_MANAGER_DICTIONARY_URL_PREFIX)
public class DictionaryApi implements DictionaryClient {
    @Resource
    private DictionaryService dictionaryService;

    @Override
    public R<List<Dictionary>> driverDictionary() {
        try {
            List<Dictionary> dictionaryList = dictionaryService.driverDictionary();
            if (null != dictionaryList) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Dictionary>> driverAttributeDictionary() {
        try {
            List<Dictionary> dictionaryList = dictionaryService.driverAttributeDictionary();
            if (null != dictionaryList) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Dictionary>> pointAttributeDictionary() {
        try {
            List<Dictionary> dictionaryList = dictionaryService.pointAttributeDictionary();
            if (null != dictionaryList) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Dictionary>> profileDictionary() {
        try {
            List<Dictionary> dictionaryList = dictionaryService.profileDictionary();
            if (null != dictionaryList) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Dictionary>> groupDictionary() {
        try {
            List<Dictionary> dictionaryList = dictionaryService.groupDictionary();
            if (null != dictionaryList) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Dictionary>> deviceDictionary(String parent) {
        try {
            List<Dictionary> dictionaryList = dictionaryService.deviceDictionary(parent);
            if (null != dictionaryList) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Dictionary>> pointDictionary(String parent) {
        try {
            List<Dictionary> dictionaryList = dictionaryService.pointDictionary(parent);
            if (null != dictionaryList) {
                return R.ok(dictionaryList);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
}
