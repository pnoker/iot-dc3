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
import io.github.pnoker.common.data.entity.bo.MessageBO;
import io.github.pnoker.common.data.entity.bo.NotifyBO;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBO;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBindBO;
import io.github.pnoker.common.data.repository.ReactiveNotifyConfigStore;
import io.github.pnoker.common.data.entity.property.AlarmCacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
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
 * @since 2026.5.21
 */
@Slf4j
@Component
public class NotifyConfigCache {

    private final ReactiveNotifyConfigStore configStore;
    private final Cache<NotifyKey, Mono<NotifyBO>> notifyCache;
    private final Cache<MessageKey, Mono<MessageBO>> messageCache;
    private final Cache<ChannelKey, Mono<NotifyChannelBO>> channelCache;
    private final Cache<NotifyBindKey, Mono<List<NotifyChannelBindBO>>> bindCache;

    public NotifyConfigCache(ReactiveNotifyConfigStore configStore, AlarmCacheProperties alarmCacheProperties) {
        this.configStore = configStore;
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
     * Returns the notify policy for {@code id}, or an empty Mono when it doesn't
     * exist. Caches the result either way so a hot rule pointing at a missing
     * policy does not pound the database.
     */
    public Mono<NotifyBO> getNotify(Long id, Long tenantId) {
        if (!isValidId(id) || !isValidId(tenantId)) return Mono.empty();
        return notifyCache.get(new NotifyKey(id, tenantId), key ->
                configStore.getNotify(key.tenantId(), key.id()).cache());
    }

    /**
     * Return message reactively.
     *
     * @param id id
     * @return get message result
     */
    public Mono<MessageBO> getMessage(Long id, Long tenantId) {
        if (!isValidId(id) || !isValidId(tenantId)) return Mono.empty();
        return messageCache.get(new MessageKey(id, tenantId), key ->
                configStore.getMessage(key.tenantId(), key.id()).cache());
    }

    /**
     * Returns the channel for {@code id} only when the lookup tenant matches
     * the channel's tenant — channel lookups are always tenant-scoped at the
     * call site, so we encode that here to keep callers from forgetting the
     * tenant guard.
     */
    public Mono<NotifyChannelBO> getChannel(Long id, Long tenantId) {
        if (!isValidId(id) || !isValidId(tenantId)) return Mono.empty();
        return channelCache.get(new ChannelKey(id, tenantId), key ->
                configStore.getChannel(key.tenantId(), key.id()).filter(channel ->
                        Objects.equals(channel.getTenantId(), key.tenantId())
                                && channel.getChannelTypeFlag() != null).cache());
    }

    /**
     * Returns enabled bindings for {@code (tenantId, notifyId)}. The result is
     * an unmodifiable list to keep accidental mutation from corrupting the cache.
     */
    public Mono<List<NotifyChannelBindBO>> findEnabledBinds(NotifyBO notify) {
        if (notify == null || !isValidId(notify.getId()) || !isValidId(notify.getTenantId())) return Mono.just(List.of());
        NotifyBindKey key = new NotifyBindKey(notify.getTenantId(), notify.getId());
        return bindCache.get(key, k -> configStore.listEnabledBinds(k.tenantId(), k.notifyId())
                .collectList().map(List::copyOf).cache());
    }

    /**
     * Invalidate notify.
     *
     * @param id id
     */
    public void invalidateNotify(Long id) {
        if (Objects.nonNull(id)) notifyCache.asMap().keySet().removeIf(key -> Objects.equals(key.id(), id));
    }

    /**
     * Invalidate message.
     *
     * @param id id
     */
    public void invalidateMessage(Long id) {
        if (Objects.nonNull(id)) messageCache.asMap().keySet().removeIf(key -> Objects.equals(key.id(), id));
    }

    /**
     * Invalidate channel.
     *
     * @param id id
     */
    public void invalidateChannel(Long id) {
        if (Objects.nonNull(id)) channelCache.asMap().keySet().removeIf(key -> Objects.equals(key.id(), id));
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

    /**
     * Cache key of one notify rule's channel bindings: tenant + notify id.
     */
    public record NotifyBindKey(Long tenantId, Long notifyId) {
    }

    public record NotifyKey(Long id, Long tenantId) {
    }

    public record MessageKey(Long id, Long tenantId) {
    }

    public record ChannelKey(Long id, Long tenantId) {
    }

}
