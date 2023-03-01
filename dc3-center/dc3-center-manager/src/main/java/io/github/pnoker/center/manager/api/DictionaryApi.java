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

package io.github.pnoker.center.manager.api;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.dto.DictionaryDto;
import io.github.pnoker.api.center.manager.feign.DictionaryClient;
import io.github.pnoker.center.manager.service.DictionaryService;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.Dictionary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerServiceConstant.DICTIONARY_URL_PREFIX)
public class DictionaryApi implements DictionaryClient {

    @Resource
    private DictionaryService dictionaryService;

    @Override
    public R<Page<Dictionary>> driverDictionary(DictionaryDto dictionaryDto, String tenantId) {
        try {
            if (ObjectUtil.isEmpty(dictionaryDto)) {
                dictionaryDto = new DictionaryDto();
            }
            dictionaryDto.setTenantId(tenantId);
            Page<Dictionary> page = dictionaryService.driverDictionary(dictionaryDto);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Page<Dictionary>> profileDictionary(DictionaryDto dictionaryDto, String tenantId) {
        try {
            if (ObjectUtil.isEmpty(dictionaryDto)) {
                dictionaryDto = new DictionaryDto();
            }
            dictionaryDto.setTenantId(tenantId);
            Page<Dictionary> page = dictionaryService.profileDictionary(dictionaryDto);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Page<Dictionary>> deviceDictionary(DictionaryDto dictionaryDto, String tenantId) {
        try {
            if (ObjectUtil.isEmpty(dictionaryDto)) {
                dictionaryDto = new DictionaryDto();
            }
            dictionaryDto.setTenantId(tenantId);
            Page<Dictionary> page = dictionaryService.deviceDictionary(dictionaryDto);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Page<Dictionary>> pointDictionary(DictionaryDto dictionaryDto, String tenantId) {
        try {
            if (ObjectUtil.isEmpty(dictionaryDto)) {
                dictionaryDto = new DictionaryDto();
            }
            dictionaryDto.setTenantId(tenantId);
            Page<Dictionary> page = dictionaryService.pointDictionary(dictionaryDto);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
}
