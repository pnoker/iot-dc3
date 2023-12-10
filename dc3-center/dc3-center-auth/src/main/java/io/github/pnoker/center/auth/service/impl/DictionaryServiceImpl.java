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

package io.github.pnoker.center.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.pnoker.center.auth.mapper.BlackIpMapper;
import io.github.pnoker.center.auth.mapper.TenantMapper;
import io.github.pnoker.center.auth.mapper.UserLoginMapper;
import io.github.pnoker.center.auth.service.DictionaryService;
import io.github.pnoker.common.entity.common.Dictionary;
import io.github.pnoker.center.auth.entity.bo.BlackIpBO;
import io.github.pnoker.center.auth.entity.bo.TenantBO;
import io.github.pnoker.center.auth.entity.bo.UserLogin;
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
    private TenantMapper tenantMapper;
    @Resource
    private UserLoginMapper userLoginMapper;
    @Resource
    private BlackIpMapper blackIpMapper;

    @Override
    public List<Dictionary> tenantDictionary() {
        List<Dictionary> dictionaryList = new ArrayList<>(16);
        LambdaQueryWrapper<TenantBO> wrapper = Wrappers.<TenantBO>query().lambda();
        List<TenantBO> tenantBOList = tenantMapper.selectList(wrapper);
        for (TenantBO tenantBO : tenantBOList) {
            Dictionary driverDictionary = new Dictionary();
            driverDictionary.setLabel(tenantBO.getTenantName());
            driverDictionary.setValue(tenantBO.getId().toString());
            dictionaryList.add(driverDictionary);
        }
        return dictionaryList;
    }

    @Override
    public List<Dictionary> userDictionary(Long tenantId) {
        List<Dictionary> dictionaryList = new ArrayList<>(16);
        LambdaQueryWrapper<UserLogin> wrapper = Wrappers.<UserLogin>query().lambda();
        List<UserLogin> userLoginList = userLoginMapper.selectList(wrapper);
        for (UserLogin userLogin : userLoginList) {
            Dictionary driverDictionary = new Dictionary();
            driverDictionary.setLabel(userLogin.getLoginName());
            driverDictionary.setValue(userLogin.getId().toString());
            dictionaryList.add(driverDictionary);
        }
        return dictionaryList;
    }

    @Override
    public List<Dictionary> blackIpDictionary(Long tenantId) {
        List<Dictionary> dictionaryList = new ArrayList<>(16);
        LambdaQueryWrapper<BlackIpBO> wrapper = Wrappers.<BlackIpBO>query().lambda();
        List<BlackIpBO> blackIpBOList = blackIpMapper.selectList(wrapper);
        for (BlackIpBO blackIpBO : blackIpBOList) {
            Dictionary driverDictionary = new Dictionary();
            driverDictionary.setLabel(blackIpBO.getIp());
            driverDictionary.setValue(blackIpBO.getId().toString());
            dictionaryList.add(driverDictionary);
        }
        return dictionaryList;
    }

}
