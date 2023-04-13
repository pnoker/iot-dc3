/*
 * Copyright 2016-present the original author or authors.
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

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.entity.query.BlackIpPageQuery;
import io.github.pnoker.center.auth.mapper.BlackIpMapper;
import io.github.pnoker.center.auth.service.BlackIpService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.model.BlackIp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 用户服务接口实现类
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class BlackIpServiceImpl implements BlackIpService {

    @Resource
    private BlackIpMapper blackIpMapper;

    @Override
    public void add(BlackIp entityDO) {
        BlackIp select = selectByIp(entityDO.getIp(), false);
        if (ObjectUtil.isNotNull(select)) {
            throw new DuplicateException("The ip already exists in the blacklist");
        }

        if (blackIpMapper.insert(entityDO) < 1) {
            throw new AddException("The ip {} add to the blacklist failed", entityDO.getIp());
        }
    }

    @Override
    public void delete(String id) {
        BlackIp blackIp = selectById(id);
        if (ObjectUtil.isNull(blackIp)) {
            throw new NotFoundException("The ip does not exist in the blacklist");
        }

        if (blackIpMapper.deleteById(id) < 1) {
            throw new DeleteException("The ip delete failed");
        }
    }

    @Override
    public void update(BlackIp entityDO) {
        entityDO.setIp(null);
        entityDO.setOperateTime(null);
        if (blackIpMapper.updateById(entityDO) < 1) {
            throw new UpdateException("The ip update failed in the blacklist");
        }
    }

    @Override
    public BlackIp selectById(String id) {
        return blackIpMapper.selectById(id);
    }

    @Override
    public BlackIp selectByIp(String ip, boolean throwException) {
        LambdaQueryWrapper<BlackIp> queryWrapper = Wrappers.<BlackIp>query().lambda();
        queryWrapper.eq(BlackIp::getIp, ip);
        queryWrapper.last("limit 1");
        BlackIp blackIp = blackIpMapper.selectOne(queryWrapper);
        if (throwException && ObjectUtil.isNull(blackIp)) {
            throw new NotFoundException();
        }
        return blackIp;
    }

    @Override
    public Page<BlackIp> list(BlackIpPageQuery queryDTO) {
        if (ObjectUtil.isNull(queryDTO.getPage())) {
            queryDTO.setPage(new Pages());
        }
        return blackIpMapper.selectPage(queryDTO.getPage().convert(), fuzzyQuery(queryDTO));
    }

    @Override
    public Boolean checkBlackIpValid(String ip) {
        BlackIp blackIp = selectByIp(ip, false);
        if (ObjectUtil.isNotNull(blackIp)) {
            return EnableFlagEnum.ENABLE.equals(blackIp.getEnableFlag());
        }
        return false;
    }

    private LambdaQueryWrapper<BlackIp> fuzzyQuery(BlackIpPageQuery query) {
        LambdaQueryWrapper<BlackIp> queryWrapper = Wrappers.<BlackIp>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            queryWrapper.like(CharSequenceUtil.isNotEmpty(query.getIp()), BlackIp::getIp, query.getIp());
        }
        return queryWrapper;
    }

}
