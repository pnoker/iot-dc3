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

package io.github.pnoker.common.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.data.dal.NotifyHistoryManager;
import io.github.pnoker.common.data.entity.bo.NotifyHistoryBO;
import io.github.pnoker.common.data.entity.builder.NotifyHistoryBuilder;
import io.github.pnoker.common.data.entity.model.NotifyHistoryDO;
import io.github.pnoker.common.data.entity.query.NotifyHistoryQuery;
import io.github.pnoker.common.data.service.NotifyHistoryService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Notification delivery history service implementation.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyHistoryServiceImpl implements NotifyHistoryService {

    private final NotifyHistoryBuilder notifyHistoryBuilder;

    private final NotifyHistoryManager notifyHistoryManager;

    @Override
    public void add(NotifyHistoryBO entityBO) {
        NotifyHistoryDO entityDO = notifyHistoryBuilder.buildDOByBO(entityBO);
        if (!notifyHistoryManager.save(entityDO)) {
            throw new AddException("Failed to create notify history");
        }
    }

    @Override
    public void delete(Long id) {
        getDOById(id, true);

        if (!notifyHistoryManager.removeById(id)) {
            throw new DeleteException("Failed to remove notify history");
        }
    }

    @Override
    public void update(NotifyHistoryBO entityBO) {
        getDOById(entityBO.getId(), true);

        NotifyHistoryDO entityDO = notifyHistoryBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!notifyHistoryManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update notify history");
        }
    }

    @Override
    public NotifyHistoryBO getById(Long id) {
        NotifyHistoryDO entityDO = getDOById(id, true);
        return notifyHistoryBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<NotifyHistoryBO> list(NotifyHistoryQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<NotifyHistoryDO> entityPageDO = notifyHistoryManager.page(
                PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return notifyHistoryBuilder.buildBOPageByDOPage(entityPageDO);
    }

    private LambdaQueryWrapper<NotifyHistoryDO> fuzzyQuery(NotifyHistoryQuery entityQuery) {
        LambdaQueryWrapper<NotifyHistoryDO> wrapper = Wrappers.<NotifyHistoryDO>query().lambda();
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getRuleId()), NotifyHistoryDO::getRuleId,
                entityQuery.getRuleId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getNotifyId()), NotifyHistoryDO::getNotifyId,
                entityQuery.getNotifyId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getMessageId()), NotifyHistoryDO::getMessageId,
                entityQuery.getMessageId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getChannelId()), NotifyHistoryDO::getChannelId,
                entityQuery.getChannelId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getEventId()), NotifyHistoryDO::getEventId,
                entityQuery.getEventId());
        wrapper.eq(Objects.nonNull(entityQuery.getChannelTypeFlag()), NotifyHistoryDO::getChannelTypeFlag,
                Objects.isNull(entityQuery.getChannelTypeFlag()) ? null
                        : entityQuery.getChannelTypeFlag().getIndex());
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getTarget()), NotifyHistoryDO::getTarget,
                entityQuery.getTarget());
        wrapper.eq(Objects.nonNull(entityQuery.getStatusFlag()), NotifyHistoryDO::getStatusFlag,
                Objects.isNull(entityQuery.getStatusFlag()) ? null : entityQuery.getStatusFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getTenantId()), NotifyHistoryDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    private NotifyHistoryDO getDOById(Long id, boolean throwException) {
        NotifyHistoryDO entityDO = notifyHistoryManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Notify history does not exist");
        }
        return entityDO;
    }

}
