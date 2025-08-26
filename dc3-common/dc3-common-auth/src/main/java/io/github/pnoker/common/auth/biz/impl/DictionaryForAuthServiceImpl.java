/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.auth.biz.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.pnoker.common.auth.biz.DictionaryForAuthService;
import io.github.pnoker.common.auth.dal.TenantManager;
import io.github.pnoker.common.auth.entity.model.TenantDO;
import io.github.pnoker.common.dal.entity.bo.DictionaryBO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DictionaryForAuthServiceImpl implements DictionaryForAuthService {

    @Resource
    private TenantManager tenantManager;

    @Override
    public List<DictionaryBO> tenantDictionary() {
        LambdaQueryWrapper<TenantDO> wrapper = Wrappers.<TenantDO>query().lambda();
        wrapper.eq(TenantDO::getEnableFlag, EnableFlagEnum.ENABLE);
        List<TenantDO> entityDOList = tenantManager.list(wrapper);

        return entityDOList.stream().map(entityDO -> {
            DictionaryBO driverDictionary = new DictionaryBO();
            driverDictionary.setLabel(entityDO.getTenantName());
            driverDictionary.setValue(entityDO.getId().toString());
            return driverDictionary;
        }).toList();
    }

}
