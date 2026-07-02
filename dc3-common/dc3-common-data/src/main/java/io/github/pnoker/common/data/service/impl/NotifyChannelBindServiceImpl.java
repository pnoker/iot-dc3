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
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.data.dal.NotifyChannelBindManager;
import io.github.pnoker.common.data.dal.NotifyChannelManager;
import io.github.pnoker.common.data.dal.NotifyManager;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBindBO;
import io.github.pnoker.common.data.entity.builder.NotifyChannelBindBuilder;
import io.github.pnoker.common.data.entity.model.NotifyChannelBindDO;
import io.github.pnoker.common.data.entity.model.NotifyChannelDO;
import io.github.pnoker.common.data.entity.model.NotifyDO;
import io.github.pnoker.common.data.entity.query.NotifyChannelBindQuery;
import io.github.pnoker.common.data.service.NotifyChannelBindService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Notification channel binding service implementation.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyChannelBindServiceImpl implements NotifyChannelBindService {

    private final NotifyChannelBindBuilder notifyChannelBindBuilder;

    private final NotifyChannelBindManager notifyChannelBindManager;

    private final NotifyManager notifyManager;

    private final NotifyChannelManager notifyChannelManager;

    private final io.github.pnoker.common.data.biz.alarm.NotifyConfigCache notifyConfigCache;

    @Override
    public void add(NotifyChannelBindBO entityBO) {
        requireReferences(entityBO);
        checkDuplicate(entityBO, false, true);

        NotifyChannelBindDO entityDO = notifyChannelBindBuilder.buildDOByBO(entityBO);
        if (!notifyChannelBindManager.save(entityDO)) {
            throw new AddException("Failed to create notify channel binding");
        }
        notifyConfigCache.invalidateBinds(entityBO.getTenantId(), entityBO.getNotifyId());
    }

    @Override
    public void delete(Long id) {
        NotifyChannelBindDO existing = getDOById(id, true);

        if (!notifyChannelBindManager.removeById(id)) {
            throw new DeleteException("Failed to remove notify channel binding");
        }
        notifyConfigCache.invalidateBinds(existing.getTenantId(), existing.getNotifyId());
    }

    @Override
    public void update(NotifyChannelBindBO entityBO) {
        NotifyChannelBindDO existing = getDOById(entityBO.getId(), true);

        requireReferences(entityBO);
        checkDuplicate(entityBO, true, true);

        NotifyChannelBindDO entityDO = notifyChannelBindBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!notifyChannelBindManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update notify channel binding");
        }
        // Invalidate both old and new (tenant, notify) tuples; the parent notify
        // could in principle change.
        notifyConfigCache.invalidateBinds(existing.getTenantId(), existing.getNotifyId());
        notifyConfigCache.invalidateBinds(entityBO.getTenantId(), entityBO.getNotifyId());
    }

    @Override
    public NotifyChannelBindBO getById(Long id) {
        NotifyChannelBindDO entityDO = getDOById(id, true);
        return notifyChannelBindBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<NotifyChannelBindBO> list(NotifyChannelBindQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<NotifyChannelBindDO> entityPageDO = notifyChannelBindManager.page(
                PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return notifyChannelBindBuilder.buildBOPageByDOPage(entityPageDO);
    }

    private LambdaQueryWrapper<NotifyChannelBindDO> fuzzyQuery(NotifyChannelBindQuery entityQuery) {
        LambdaQueryWrapper<NotifyChannelBindDO> wrapper = Wrappers.<NotifyChannelBindDO>query().lambda();
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getNotifyId()), NotifyChannelBindDO::getNotifyId,
                entityQuery.getNotifyId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getChannelId()), NotifyChannelBindDO::getChannelId,
                entityQuery.getChannelId());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), NotifyChannelBindDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        return wrapper;
    }

    private boolean checkDuplicate(NotifyChannelBindBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<NotifyChannelBindDO> wrapper = Wrappers.<NotifyChannelBindDO>query().lambda();
        wrapper.eq(NotifyChannelBindDO::getNotifyId, entityBO.getNotifyId());
        wrapper.eq(NotifyChannelBindDO::getChannelId, entityBO.getChannelId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        NotifyChannelBindDO one = notifyChannelBindManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Notify channel binding has been duplicated");
        }
        return duplicate;
    }

    private void requireReferences(NotifyChannelBindBO entityBO) {
        NotifyDO notify = notifyManager.getById(entityBO.getNotifyId());
        if (Objects.isNull(notify) || !Objects.equals(notify.getTenantId(), entityBO.getTenantId())) {
            throw new NotFoundException("Notify policy does not exist");
        }

        NotifyChannelDO channel = notifyChannelManager.getById(entityBO.getChannelId());
        if (Objects.isNull(channel) || !Objects.equals(channel.getTenantId(), entityBO.getTenantId())) {
            throw new NotFoundException("Notify channel does not exist");
        }
    }

    private NotifyChannelBindDO getDOById(Long id, boolean throwException) {
        NotifyChannelBindDO entityDO = notifyChannelBindManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Notify channel binding does not exist");
        }
        return entityDO;
    }

}
