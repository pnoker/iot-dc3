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

import io.github.pnoker.common.data.biz.alarm.RuleRegistry;
import io.github.pnoker.common.data.entity.builder.MessageBuilder;
import io.github.pnoker.common.data.entity.builder.NotifyBuilder;
import io.github.pnoker.common.data.entity.builder.NotifyChannelBindBuilder;
import io.github.pnoker.common.data.entity.builder.NotifyChannelBuilder;
import io.github.pnoker.common.data.entity.builder.RuleBuilder;
import io.github.pnoker.common.data.entity.query.MessageQuery;
import io.github.pnoker.common.data.entity.query.NotifyChannelBindQuery;
import io.github.pnoker.common.data.entity.query.NotifyChannelQuery;
import io.github.pnoker.common.data.entity.query.NotifyQuery;
import io.github.pnoker.common.data.entity.query.RuleQuery;
import io.github.pnoker.common.data.repository.ReactiveRuleStore;
import io.github.pnoker.common.data.repository.ReactiveNotifyAdminStore;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class AlarmListQueryServiceTest {

    @Mock
    private RuleBuilder ruleBuilder;

    @Mock
    private ReactiveRuleStore ruleStore;

    @Mock
    private RuleRegistry ruleRegistry;

    @Mock
    private NotifyBuilder notifyBuilder;

    @Mock
    private ReactiveNotifyAdminStore notifyAdminStore;

    @Mock
    private MessageBuilder messageBuilder;

    @Mock
    private NotifyChannelBuilder notifyChannelBuilder;

    @Mock
    private NotifyChannelBindBuilder notifyChannelBindBuilder;

    @InjectMocks
    private RuleServiceImpl ruleService;

    @InjectMocks
    private NotifyServiceImpl notifyService;

    @InjectMocks
    private MessageServiceImpl messageService;

    @InjectMocks
    private NotifyChannelServiceImpl notifyChannelService;

    @InjectMocks
    private NotifyChannelBindServiceImpl notifyChannelBindService;

    @Test
    void alarmListQueriesAllowMissingOptionalFilters() {
        Mockito.when(ruleStore.list(1L, null, null, null, null, null,
                        new io.github.pnoker.db.r2dbc.core.page.PageRequest(0, 50)))
                .thenReturn(Mono.just(OffsetPage.of(java.util.List.of(), 0, 50, 0)));
        OffsetPage<?> empty = OffsetPage.of(java.util.List.of(), 0, 50, 0);
        Mockito.lenient().when(notifyAdminStore.listNotify(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn((Mono) Mono.just(empty));
        Mockito.lenient().when(notifyAdminStore.listMessage(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn((Mono) Mono.just(empty));
        Mockito.lenient().when(notifyAdminStore.listChannel(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn((Mono) Mono.just(empty));
        Mockito.lenient().when(notifyAdminStore.listBind(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn((Mono) Mono.just(empty));
        assertThatCode(() -> ruleService.list(1L, new RuleQuery()).block()).doesNotThrowAnyException();
        assertThatCode(() -> notifyService.list(1L, new NotifyQuery()).block()).doesNotThrowAnyException();
        assertThatCode(() -> messageService.list(1L, new MessageQuery()).block()).doesNotThrowAnyException();
        assertThatCode(() -> notifyChannelService.list(1L, new NotifyChannelQuery()).block()).doesNotThrowAnyException();
        assertThatCode(() -> notifyChannelBindService.list(1L, new NotifyChannelBindQuery()).block())
                .doesNotThrowAnyException();
    }

}
