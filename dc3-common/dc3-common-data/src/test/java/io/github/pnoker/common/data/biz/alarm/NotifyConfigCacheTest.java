package io.github.pnoker.common.data.biz.alarm;

import io.github.pnoker.common.data.entity.bo.MessageBO;
import io.github.pnoker.common.data.entity.bo.NotifyBO;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBO;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBindBO;
import io.github.pnoker.common.data.repository.ReactiveNotifyConfigStore;
import io.github.pnoker.common.data.entity.property.AlarmCacheProperties;
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyConfigCacheTest {

    @Mock
    private ReactiveNotifyConfigStore configStore;

    private NotifyConfigCache cache;

    @BeforeEach
    void setUp() {
        cache = new NotifyConfigCache(configStore, new AlarmCacheProperties());
    }

    @Test
    void cachesNotifyAcrossLookups() {
        NotifyBO notify = new NotifyBO();
        notify.setId(1L);
        notify.setTenantId(7L);
        when(configStore.getNotify(7L, 1L)).thenReturn(Mono.just(notify));

        NotifyBO first = cache.getNotify(1L, 7L).block();
        NotifyBO second = cache.getNotify(1L, 7L).block();

        assertThat(first).isSameAs(notify);
        assertThat(second).isSameAs(notify);
        verify(configStore, times(1)).getNotify(7L, 1L);
    }

    @Test
    void invalidateNotifyForcesReload() {
        NotifyBO notify = new NotifyBO();
        notify.setId(1L);
        notify.setTenantId(7L);
        when(configStore.getNotify(7L, 1L)).thenReturn(Mono.just(notify));

        cache.getNotify(1L, 7L).block();
        cache.invalidateNotify(1L);
        cache.getNotify(1L, 7L).block();

        verify(configStore, times(2)).getNotify(7L, 1L);
    }

    @Test
    void cachesMessageAcrossLookups() {
        MessageBO message = new MessageBO();
        message.setId(2L);
        message.setTenantId(7L);
        when(configStore.getMessage(7L, 2L)).thenReturn(Mono.just(message));

        cache.getMessage(2L, 7L).block();
        cache.getMessage(2L, 7L).block();

        verify(configStore, times(1)).getMessage(7L, 2L);
    }

    @Test
    void channelLookupEnforcesTenantScope() {
        NotifyChannelBO channel = new NotifyChannelBO();
        channel.setId(3L);
        channel.setTenantId(7L);
        channel.setChannelTypeFlag(NotifyChannelTypeEnum.FEISHU_BOT);
        when(configStore.getChannel(7L, 3L)).thenReturn(Mono.just(channel));
        when(configStore.getChannel(9L, 3L)).thenReturn(Mono.empty());

        assertThat(cache.getChannel(3L, 7L).block()).isSameAs(channel);
        assertThat(cache.getChannel(3L, 9L).block()).isNull();
        verify(configStore).getChannel(9L, 3L);
    }

    @Test
    void cachesEnabledBindsByTenantAndNotify() {
        NotifyBO notify = new NotifyBO();
        notify.setId(1L);
        notify.setTenantId(7L);
        NotifyChannelBindBO bind = new NotifyChannelBindBO();
        bind.setNotifyId(1L);
        bind.setTenantId(7L);
        when(configStore.listEnabledBinds(7L, 1L)).thenReturn(Flux.just(bind));

        assertThat(cache.findEnabledBinds(notify).block()).containsExactly(bind);
        assertThat(cache.findEnabledBinds(notify).block()).containsExactly(bind);
        verify(configStore, times(1)).listEnabledBinds(7L, 1L);
    }

    @Test
    void invalidateAllDropsEverySection() {
        NotifyBO notify = new NotifyBO();
        notify.setId(1L);
        notify.setTenantId(7L);
        MessageBO message = new MessageBO();
        message.setId(2L);
        message.setTenantId(7L);
        when(configStore.getNotify(7L, 1L)).thenReturn(Mono.just(notify));
        when(configStore.getMessage(7L, 2L)).thenReturn(Mono.just(message));

        cache.getNotify(1L, 7L).block();
        cache.getMessage(2L, 7L).block();
        cache.invalidateAll();
        cache.getNotify(1L, 7L).block();
        cache.getMessage(2L, 7L).block();

        verify(configStore, times(2)).getNotify(7L, 1L);
        verify(configStore, times(2)).getMessage(7L, 2L);
    }

    @Test
    void rejectsZeroAndNullIds() {
        cache.getNotify(0L, 7L).block();
        cache.getNotify(null, 7L).block();
        cache.getMessage(0L, 7L).block();
        cache.getMessage(null, 7L).block();
        cache.getChannel(0L, 7L).block();
        cache.getChannel(null, 7L).block();

        verifyNoInteractions(configStore);
    }
}
