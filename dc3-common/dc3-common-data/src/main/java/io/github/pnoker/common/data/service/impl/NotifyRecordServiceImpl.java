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
import io.github.pnoker.common.data.dal.NotifyRecordManager;
import io.github.pnoker.common.data.entity.bo.NotifyRecordBO;
import io.github.pnoker.common.data.entity.builder.NotifyRecordBuilder;
import io.github.pnoker.common.data.entity.model.NotifyRecordDO;
import io.github.pnoker.common.data.entity.query.NotifyRecordQuery;
import io.github.pnoker.common.data.service.NotifyRecordService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Notification delivery record service implementation.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
public class NotifyRecordServiceImpl implements NotifyRecordService {

    @Resource
    private NotifyRecordBuilder notifyRecordBuilder;

    @Resource
    private NotifyRecordManager notifyRecordManager;

    @Override
    public void save(NotifyRecordBO entityBO) {
        NotifyRecordDO entityDO = notifyRecordBuilder.buildDOByBO(entityBO);
        if (!notifyRecordManager.save(entityDO)) {
            throw new AddException("Failed to create notify record");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!notifyRecordManager.removeById(id)) {
            throw new DeleteException("Failed to remove notify record");
        }
    }

    @Override
    public void update(NotifyRecordBO entityBO) {
        getDOById(entityBO.getId(), true);

        NotifyRecordDO entityDO = notifyRecordBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!notifyRecordManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update notify record");
        }
    }

    @Override
    public NotifyRecordBO selectById(Long id) {
        NotifyRecordDO entityDO = getDOById(id, true);
        return notifyRecordBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<NotifyRecordBO> selectByPage(NotifyRecordQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<NotifyRecordDO> entityPageDO = notifyRecordManager.page(
                PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return notifyRecordBuilder.buildBOPageByDOPage(entityPageDO);
    }

    private LambdaQueryWrapper<NotifyRecordDO> fuzzyQuery(NotifyRecordQuery entityQuery) {
        LambdaQueryWrapper<NotifyRecordDO> wrapper = Wrappers.<NotifyRecordDO>query().lambda();
        wrapper.eq(Objects.nonNull(entityQuery.getRuleId()), NotifyRecordDO::getRuleId, entityQuery.getRuleId());
        wrapper.eq(Objects.nonNull(entityQuery.getNotifyId()), NotifyRecordDO::getNotifyId,
                entityQuery.getNotifyId());
        wrapper.eq(Objects.nonNull(entityQuery.getMessageId()), NotifyRecordDO::getMessageId,
                entityQuery.getMessageId());
        wrapper.eq(Objects.nonNull(entityQuery.getChannelId()), NotifyRecordDO::getChannelId,
                entityQuery.getChannelId());
        wrapper.eq(Objects.nonNull(entityQuery.getEventId()), NotifyRecordDO::getEventId, entityQuery.getEventId());
        wrapper.eq(Objects.nonNull(entityQuery.getChannelTypeFlag()), NotifyRecordDO::getChannelTypeFlag,
                Objects.nonNull(entityQuery.getChannelTypeFlag())
                        ? entityQuery.getChannelTypeFlag().getIndex()
                        : null);
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getTarget()), NotifyRecordDO::getTarget,
                entityQuery.getTarget());
        wrapper.eq(Objects.nonNull(entityQuery.getStatusFlag()), NotifyRecordDO::getStatusFlag,
                Objects.nonNull(entityQuery.getStatusFlag()) ? entityQuery.getStatusFlag().getIndex() : null);
        wrapper.eq(Objects.nonNull(entityQuery.getTenantId()), NotifyRecordDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    private NotifyRecordDO getDOById(Long id, boolean throwException) {
        NotifyRecordDO entityDO = notifyRecordManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Notify record does not exist");
        }
        return entityDO;
    }

}
