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

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.mapper.BlackIpMapper;
import io.github.pnoker.center.auth.service.BlackIpService;
import io.github.pnoker.common.bean.Pages;
import io.github.pnoker.common.dto.BlackIpDto;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.BlackIp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 用户服务接口实现类
 *
 * @author pnoker
 */
@Slf4j
@Service
public class BlackIpServiceImpl implements BlackIpService {

    @Resource
    private BlackIpMapper blackIpMapper;

    @Override
    public BlackIp add(BlackIp blackIp) {
        BlackIp select = selectByIp(blackIp.getIp());
        if (ObjectUtil.isNotNull(select)) {
            throw new ServiceException("The ip already exists in the blacklist");
        }
        if (blackIpMapper.insert(blackIp) > 0) {
            return blackIpMapper.selectById(blackIp.getId());
        }
        throw new ServiceException("The ip add to the blacklist failed");
    }

    @Override
    public boolean delete(String id) {
        BlackIp blackIp = selectById(id);
        if (null == blackIp) {
            throw new ServiceException("The ip does not exist in the blacklist");
        }
        return blackIpMapper.deleteById(id) > 0;
    }

    @Override
    public BlackIp update(BlackIp blackIp) {
        blackIp.setIp(null).setUpdateTime(null);
        if (blackIpMapper.updateById(blackIp) > 0) {
            BlackIp select = blackIpMapper.selectById(blackIp.getId());
            blackIp.setIp(select.getIp());
            return select;
        }
        throw new ServiceException("The ip update failed in the blacklist");
    }

    @Override
    public BlackIp selectById(String id) {
        return blackIpMapper.selectById(id);
    }

    @Override
    public BlackIp selectByIp(String ip) {
        LambdaQueryWrapper<BlackIp> queryWrapper = Wrappers.<BlackIp>query().lambda();
        queryWrapper.eq(BlackIp::getIp, ip);
        return blackIpMapper.selectOne(queryWrapper);
    }

    @Override
    public Page<BlackIp> list(BlackIpDto blackIpDto) {
        if (null == blackIpDto.getPage()) {
            blackIpDto.setPage(new Pages());
        }
        return blackIpMapper.selectPage(blackIpDto.getPage().convert(), fuzzyQuery(blackIpDto));
    }

    @Override
    public boolean checkBlackIpValid(String ip) {
        BlackIp blackIp = selectByIp(ip);
        if (ObjectUtil.isNotNull(blackIp)) {
            return blackIp.getEnable();
        }
        return false;
    }

    @Override
    public LambdaQueryWrapper<BlackIp> fuzzyQuery(BlackIpDto blackIpDto) {
        LambdaQueryWrapper<BlackIp> queryWrapper = Wrappers.<BlackIp>query().lambda();
        if (ObjectUtil.isNotNull(blackIpDto)) {
            queryWrapper.like(StrUtil.isNotEmpty(blackIpDto.getIp()), BlackIp::getIp, blackIpDto.getIp());
        }
        return queryWrapper;
    }

}
