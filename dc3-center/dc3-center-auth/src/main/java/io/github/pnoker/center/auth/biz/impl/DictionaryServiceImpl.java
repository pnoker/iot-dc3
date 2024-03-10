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

package io.github.pnoker.center.auth.biz.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.pnoker.center.auth.biz.DictionaryService;
import io.github.pnoker.center.auth.dal.LimitedIpManager;
import io.github.pnoker.center.auth.dal.TenantManager;
import io.github.pnoker.center.auth.entity.model.LimitedIpDO;
import io.github.pnoker.center.auth.entity.model.TenantDO;
import io.github.pnoker.common.entity.bo.DictionaryBO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DictionaryServiceImpl implements DictionaryService {

    @Resource
    private TenantManager tenantManager;
    @Resource
    private LimitedIpManager limitedIpManager;

    @Override
    public List<DictionaryBO> tenantDictionary() {
        LambdaQueryWrapper<TenantDO> wrapper = Wrappers.<TenantDO>query().lambda();
        wrapper.eq(TenantDO::getEnableFlag, EnableFlagEnum.ENABLE);
        List<TenantDO> entityDOS = tenantManager.list(wrapper);

        return entityDOS.stream().map(entityDO -> {
            DictionaryBO driverDictionary = new DictionaryBO();
            driverDictionary.setLabel(entityDO.getTenantName());
            driverDictionary.setValue(entityDO.getId());
            return driverDictionary;
        }).collect(Collectors.toList());
    }

    @Override
    public List<DictionaryBO> limitedIpDictionary(Long tenantId) {
        LambdaQueryWrapper<LimitedIpDO> wrapper = Wrappers.<LimitedIpDO>query().lambda();
        wrapper.eq(LimitedIpDO::getTenantId, tenantId);
        wrapper.eq(LimitedIpDO::getEnableFlag, EnableFlagEnum.ENABLE);
        List<LimitedIpDO> entityDOS = limitedIpManager.list(wrapper);

        return entityDOS.stream().map(entityDO -> {
            DictionaryBO driverDictionary = new DictionaryBO();
            driverDictionary.setLabel(entityDO.getIp());
            driverDictionary.setValue(entityDO.getId());
            return driverDictionary;
        }).collect(Collectors.toList());
    }

}
