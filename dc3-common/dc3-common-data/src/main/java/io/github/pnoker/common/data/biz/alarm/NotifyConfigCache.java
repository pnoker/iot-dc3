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

package io.github.pnoker.common.data.biz.alarm;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.pnoker.common.data.dal.MessageManager;
import io.github.pnoker.common.data.dal.NotifyChannelBindManager;
import io.github.pnoker.common.data.dal.NotifyChannelManager;
import io.github.pnoker.common.data.dal.NotifyManager;
import io.github.pnoker.common.data.entity.bo.MessageBO;
import io.github.pnoker.common.data.entity.bo.NotifyBO;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBO;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBindBO;
import io.github.pnoker.common.data.entity.builder.MessageBuilder;
import io.github.pnoker.common.data.entity.builder.NotifyBuilder;
import io.github.pnoker.common.data.entity.builder.NotifyChannelBindBuilder;
import io.github.pnoker.common.data.entity.builder.NotifyChannelBuilder;
import io.github.pnoker.common.data.entity.model.MessageDO;
import io.github.pnoker.common.data.entity.model.NotifyChannelBindDO;
import io.github.pnoker.common.data.entity.model.NotifyChannelDO;
import io.github.pnoker.common.data.entity.model.NotifyDO;
import io.github.pnoker.common.data.entity.property.AlarmCacheProperties;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Caches notify-policy / message-template / channel / channel-bind entities so
 * the rule notification path does not query the database on every alarm fan-out.
 * These four entities are mutated rarely (configuration time) and read on every
 * notification — perfect cache shape.
 *
 * <p>Bind list is keyed on {@code (tenantId, notifyId)} because a notify policy
 * has 1:N bindings and the consumer always needs them as a list.
 *
 * <p>Invalidation is event-driven: the corresponding {@code *ServiceImpl}
 * classes call into this cache on add/update/delete. The configured TTL is a
 * safety net only.
 *
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
@Slf4j
@Component
public class NotifyConfigCache {

    private final NotifyManager notifyManager;
    private final NotifyBuilder notifyBuilder;
    private final MessageManager messageManager;
    private final MessageBuilder messageBuilder;
    private final NotifyChannelManager notifyChannelManager;
    private final NotifyChannelBuilder notifyChannelBuilder;
    private final NotifyChannelBindManager notifyChannelBindManager;
    private final NotifyChannelBindBuilder notifyChannelBindBuilder;

    private final Cache<Long, NotifyBO> notifyCache;
    private final Cache<Long, MessageBO> messageCache;
    private final Cache<Long, NotifyChannelBO> channelCache;
    private final Cache<NotifyBindKey, List<NotifyChannelBindBO>> bindCache;

    public NotifyConfigCache(NotifyManager notifyManager, NotifyBuilder notifyBuilder,
                             MessageManager messageManager, MessageBuilder messageBuilder,
                             NotifyChannelManager notifyChannelManager, NotifyChannelBuilder notifyChannelBuilder,
                             NotifyChannelBindManager notifyChannelBindManager,
                             NotifyChannelBindBuilder notifyChannelBindBuilder,
                             AlarmCacheProperties alarmCacheProperties) {
        this.notifyManager = notifyManager;
        this.notifyBuilder = notifyBuilder;
        this.messageManager = messageManager;
        this.messageBuilder = messageBuilder;
        this.notifyChannelManager = notifyChannelManager;
        this.notifyChannelBuilder = notifyChannelBuilder;
        this.notifyChannelBindManager = notifyChannelBindManager;
        this.notifyChannelBindBuilder = notifyChannelBindBuilder;
        AlarmCacheProperties.CacheTuning tuning = alarmCacheProperties.getNotify();
        this.notifyCache = newCache(tuning);
        this.messageCache = newCache(tuning);
        this.channelCache = newCache(tuning);
        this.bindCache = newCache(tuning);
    }

    private static <K, V> Cache<K, V> newCache(AlarmCacheProperties.CacheTuning tuning) {
        return Caffeine.newBuilder()
                .maximumSize(tuning.getMaxSize())
                .expireAfterWrite(Duration.ofSeconds(tuning.getTtlSeconds()))
                .build();
    }

    private static boolean isValidId(Long id) {
        return Objects.nonNull(id) && id > 0;
    }

    /**
     * Returns the notify policy for {@code id}, or {@code null} when it doesn't
     * exist. Caches the result either way so a hot rule pointing at a missing
     * policy does not pound the database.
     */
    public NotifyBO getNotify(Long id) {
        if (!isValidId(id)) {
            return null;
        }
        NotifyBO bo = notifyCache.get(id, key -> {
            NotifyDO entity = notifyManager.getById(key);
            return Objects.nonNull(entity) ? notifyBuilder.buildBOByDO(entity) : null;
        });
        return bo;
    }

    public MessageBO getMessage(Long id) {
        if (!isValidId(id)) {
            return null;
        }
        return messageCache.get(id, key -> {
            MessageDO entity = messageManager.getById(key);
            return Objects.nonNull(entity) ? messageBuilder.buildBOByDO(entity) : null;
        });
    }

    /**
     * Returns the channel for {@code id} only when the lookup tenant matches
     * the channel's tenant — channel lookups are always tenant-scoped at the
     * call site, so we encode that here to keep callers from forgetting the
     * tenant guard.
     */
    public NotifyChannelBO getChannel(Long id, Long tenantId) {
        if (!isValidId(id)) {
            return null;
        }
        NotifyChannelBO bo = channelCache.get(id, key -> {
            NotifyChannelDO entity = notifyChannelManager.getById(key);
            if (Objects.isNull(entity) || Objects.isNull(entity.getChannelTypeFlag())) {
                return null;
            }
            return notifyChannelBuilder.buildBOByDO(entity);
        });
        if (Objects.isNull(bo)) {
            return null;
        }
        if (!Objects.equals(bo.getTenantId(), tenantId)) {
            return null;
        }
        return bo;
    }

    /**
     * Returns enabled bindings for {@code (tenantId, notifyId)}. The result is
     * an unmodifiable list to keep accidental mutation from corrupting the cache.
     */
    public List<NotifyChannelBindBO> findEnabledBinds(NotifyBO notify) {
        if (Objects.isNull(notify) || !isValidId(notify.getId()) || !isValidId(notify.getTenantId())) {
            return List.of();
        }
        NotifyBindKey key = new NotifyBindKey(notify.getTenantId(), notify.getId());
        List<NotifyChannelBindBO> list = bindCache.get(key, k -> {
            List<NotifyChannelBindDO> rows = notifyChannelBindManager.lambdaQuery()
                    .eq(NotifyChannelBindDO::getNotifyId, k.notifyId())
                    .eq(NotifyChannelBindDO::getTenantId, k.tenantId())
                    .eq(NotifyChannelBindDO::getEnableFlag, EnableFlagEnum.ENABLE.getIndex())
                    .list();
            return Collections.unmodifiableList(notifyChannelBindBuilder.buildBOListByDOList(rows));
        });
        return Objects.requireNonNullElse(list, List.of());
    }

    public void invalidateNotify(Long id) {
        if (Objects.nonNull(id)) {
            notifyCache.invalidate(id);
        }
    }

    public void invalidateMessage(Long id) {
        if (Objects.nonNull(id)) {
            messageCache.invalidate(id);
        }
    }

    public void invalidateChannel(Long id) {
        if (Objects.nonNull(id)) {
            channelCache.invalidate(id);
        }
    }

    /**
     * Drops the {@code (tenantId, notifyId)} binding list. When a binding row
     * mutates we don't know its old/new notifyId pairing without reading from
     * the cached row; passing both is required so we drop the right entry.
     */
    public void invalidateBinds(Long tenantId, Long notifyId) {
        if (Objects.nonNull(tenantId) && Objects.nonNull(notifyId)) {
            bindCache.invalidate(new NotifyBindKey(tenantId, notifyId));
        }
    }

    /**
     * Drops every cached entry. Useful for tests and admin reset endpoints.
     */
    public void invalidateAll() {
        notifyCache.invalidateAll();
        messageCache.invalidateAll();
        channelCache.invalidateAll();
        bindCache.invalidateAll();
    }

    public record NotifyBindKey(Long tenantId, Long notifyId) {
    }

}
