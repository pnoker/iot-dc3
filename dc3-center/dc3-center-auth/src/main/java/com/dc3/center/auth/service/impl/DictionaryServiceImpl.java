/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dc3.center.auth.mapper.BlackIpMapper;
import com.dc3.center.auth.mapper.TenantMapper;
import com.dc3.center.auth.mapper.UserMapper;
import com.dc3.center.auth.service.DictionaryService;
import com.dc3.common.bean.Dictionary;
import com.dc3.common.constant.CacheConstant;
import com.dc3.common.model.BlackIp;
import com.dc3.common.model.Tenant;
import com.dc3.common.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class DictionaryServiceImpl implements DictionaryService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private TenantMapper tenantMapper;
    @Resource
    private BlackIpMapper blackIpMapper;

    @Override
    @Cacheable(value = CacheConstant.Entity.TENANT + CacheConstant.Suffix.DIC, key = "'dic'", unless = "#result==null")
    public List<Dictionary> tenantDictionary() {
        List<Dictionary> dictionaryList = new ArrayList<>(16);
        LambdaQueryWrapper<Tenant> queryWrapper = Wrappers.<Tenant>query().lambda();
        List<Tenant> tenantList = tenantMapper.selectList(queryWrapper);
        for (Tenant tenant : tenantList) {
            Dictionary driverDictionary = new Dictionary().setLabel(tenant.getName()).setValue(tenant.getId());
            dictionaryList.add(driverDictionary);
        }
        return dictionaryList;
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.USER + CacheConstant.Suffix.DIC, key = "'dic.'+#tenantId", unless = "#result==null")
    public List<Dictionary> userDictionary(Long tenantId) {
        List<Dictionary> dictionaryList = new ArrayList<>(16);
        LambdaQueryWrapper<User> queryWrapper = Wrappers.<User>query().lambda();
        List<User> userList = userMapper.selectList(queryWrapper);
        for (User user : userList) {
            Dictionary driverDictionary = new Dictionary().setLabel(user.getName()).setValue(user.getId());
            dictionaryList.add(driverDictionary);
        }
        return dictionaryList;
    }

    @Override
    @Cacheable(value = CacheConstant.Entity.BLACK_IP + CacheConstant.Suffix.DIC, key = "'dic.'+#tenantId", unless = "#result==null")
    public List<Dictionary> blackIpDictionary(Long tenantId) {
        List<Dictionary> dictionaryList = new ArrayList<>(16);
        LambdaQueryWrapper<BlackIp> queryWrapper = Wrappers.<BlackIp>query().lambda();
        List<BlackIp> blackIpList = blackIpMapper.selectList(queryWrapper);
        for (BlackIp blackIp : blackIpList) {
            Dictionary driverDictionary = new Dictionary().setLabel(blackIp.getIp()).setValue(blackIp.getId());
            dictionaryList.add(driverDictionary);
        }
        return dictionaryList;
    }

}
