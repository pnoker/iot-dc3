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
import io.github.pnoker.center.auth.dal.UserLoginManager;
import io.github.pnoker.center.auth.entity.builder.DictionaryForAuthBuilder;
import io.github.pnoker.center.auth.entity.model.LimitedIpDO;
import io.github.pnoker.center.auth.entity.model.TenantDO;
import io.github.pnoker.center.auth.entity.model.UserLoginDO;
import io.github.pnoker.common.entity.bo.DictionaryBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DictionaryServiceImpl implements DictionaryService {

    @Resource
    private DictionaryForAuthBuilder dictionaryBuilder;

    @Resource
    private TenantManager tenantManager;
    @Resource
    private UserLoginManager userLoginManager;
    @Resource
    private LimitedIpManager limitedIpManager;

    @Override
    public List<DictionaryBO> tenantDictionary() {
        List<DictionaryBO> dictionaryList = new ArrayList<>(16);
        LambdaQueryWrapper<TenantDO> wrapper = Wrappers.<TenantDO>query().lambda();
        List<TenantDO> tenantBOList = tenantManager.list(wrapper);
        for (TenantDO tenantBO : tenantBOList) {
            DictionaryBO driverDictionary = new DictionaryBO();
            driverDictionary.setLabel(tenantBO.getTenantName());
            driverDictionary.setValue(tenantBO.getId());
            dictionaryList.add(driverDictionary);
        }
        return dictionaryList;
    }

    @Override
    public List<DictionaryBO> userLoginDictionary(Long tenantId) {
        List<DictionaryBO> dictionaryList = new ArrayList<>(16);
        LambdaQueryWrapper<UserLoginDO> wrapper = Wrappers.<UserLoginDO>query().lambda();
        List<UserLoginDO> userLoginList = userLoginManager.list(wrapper);
        for (UserLoginDO userLogin : userLoginList) {
            DictionaryBO driverDictionary = new DictionaryBO();
            driverDictionary.setLabel(userLogin.getLoginName());
            driverDictionary.setValue(userLogin.getId());
            dictionaryList.add(driverDictionary);
        }
        return dictionaryList;
    }

    @Override
    public List<DictionaryBO> limitedIpDictionary(Long tenantId) {
        List<DictionaryBO> dictionaryList = new ArrayList<>(16);
        LambdaQueryWrapper<LimitedIpDO> wrapper = Wrappers.<LimitedIpDO>query().lambda();
        List<LimitedIpDO> limitedIpBOList = limitedIpManager.list(wrapper);
        for (LimitedIpDO limitedIpBO : limitedIpBOList) {
            DictionaryBO driverDictionary = new DictionaryBO();
            driverDictionary.setLabel(limitedIpBO.getIp());
            driverDictionary.setValue(limitedIpBO.getId());
            dictionaryList.add(driverDictionary);
        }
        return dictionaryList;
    }

}
