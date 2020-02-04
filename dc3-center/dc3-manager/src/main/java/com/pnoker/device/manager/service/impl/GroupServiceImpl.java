/*
 * Copyright 2019 Pnoker. All Rights Reserved.
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

package com.pnoker.device.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.DeviceDto;
import com.pnoker.common.dto.GroupDto;
import com.pnoker.common.exception.ServiceException;
import com.pnoker.common.model.Device;
import com.pnoker.common.model.Dic;
import com.pnoker.common.model.Group;
import com.pnoker.device.manager.mapper.GroupMapper;
import com.pnoker.device.manager.service.DeviceService;
import com.pnoker.device.manager.service.GroupService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * <p>设备分组服务接口实现类
 *
 * @author pnoker
 */
@Slf4j
@Service
public class GroupServiceImpl implements GroupService {

    @Resource
    private DeviceService deviceService;

    @Resource
    private GroupMapper groupMapper;

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.GROUP_ID, key = "#group.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.GROUP_NAME, key = "#group.name", condition = "#result!=null")
            },
            evict = {@CacheEvict(value = Common.Cache.GROUP_LIST, allEntries = true, condition = "#result!=null")}
    )
    public Group add(Group group) {
        Group select = selectByName(group.getName());
        if (null != select) {
            throw new ServiceException("device group already exists");
        }
        if (groupMapper.insert(group) > 0) {
            return groupMapper.selectById(group.getId());
        }
        return null;
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(value = Common.Cache.GROUP_ID, key = "#id", condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.GROUP_NAME, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.GROUP_DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.GROUP_LIST, allEntries = true, condition = "#result==true")
            }
    )
    public boolean delete(Long id) {
        DeviceDto deviceDto = new DeviceDto();
        deviceDto.setGroupId(id);
        Page<Device> devicePage = deviceService.list(deviceDto);
        if (devicePage.getTotal() > 0) {
            throw new ServiceException("group already bound by the device");
        }
        return groupMapper.deleteById(id) > 0;
    }

    @Override
    @Caching(
            put = {
                    @CachePut(value = Common.Cache.GROUP_ID, key = "#group.id", condition = "#result!=null"),
                    @CachePut(value = Common.Cache.GROUP_NAME, key = "#group.name", condition = "#result!=null")
            },
            evict = {
                    @CacheEvict(value = Common.Cache.GROUP_DIC, allEntries = true, condition = "#result==true"),
                    @CacheEvict(value = Common.Cache.GROUP_LIST, allEntries = true, condition = "#result!=null")
            }
    )
    public Group update(Group group) {
        group.setUpdateTime(null);
        if (groupMapper.updateById(group) > 0) {
            Group select = selectById(group.getId());
            group.setName(select.getName());
            return select;
        }
        return null;
    }

    @Override
    @Cacheable(value = Common.Cache.GROUP_ID, key = "#id", unless = "#result==null")
    public Group selectById(Long id) {
        return groupMapper.selectById(id);
    }

    @Override
    @Cacheable(value = Common.Cache.GROUP_NAME, key = "#name", unless = "#result==null")
    public Group selectByName(String name) {
        LambdaQueryWrapper<Group> queryWrapper = Wrappers.<Group>query().lambda();
        queryWrapper.like(Group::getName, name);
        return groupMapper.selectOne(queryWrapper);
    }

    @Override
    @Cacheable(value = Common.Cache.GROUP_DIC, key = "'group_dic'", unless = "#result==null")
    public List<Dic> groupDic() {
        return groupMapper.groupDic();
    }

    @Override
    @Cacheable(value = Common.Cache.GROUP_LIST, keyGenerator = "commonKeyGenerator", unless = "#result==null")
    public Page<Group> list(GroupDto groupDto) {
        return groupMapper.selectPage(groupDto.getPage().convert(), fuzzyQuery(groupDto));
    }

    @Override
    public LambdaQueryWrapper<Group> fuzzyQuery(GroupDto groupDto) {
        LambdaQueryWrapper<Group> queryWrapper = Wrappers.<Group>query().lambda();
        Optional.ofNullable(groupDto).ifPresent(dto -> {
            if (StringUtils.isNotBlank(dto.getName())) {
                queryWrapper.like(Group::getName, dto.getName());
            }
        });
        return queryWrapper;
    }

}
