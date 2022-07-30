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

package io.github.pnoker.center.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.pnoker.center.auth.mapper.BlackIpMapper;
import io.github.pnoker.center.auth.mapper.TenantMapper;
import io.github.pnoker.center.auth.mapper.UserMapper;
import io.github.pnoker.center.auth.service.DictionaryService;
import io.github.pnoker.common.bean.Dictionary;
import io.github.pnoker.common.model.BlackIp;
import io.github.pnoker.common.model.Tenant;
import io.github.pnoker.common.model.User;
import lombok.extern.slf4j.Slf4j;
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
    private TenantMapper tenantMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private BlackIpMapper blackIpMapper;

    @Override
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
    public List<Dictionary> userDictionary(String tenantId) {
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
    public List<Dictionary> blackIpDictionary(String tenantId) {
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
