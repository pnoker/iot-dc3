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

import io.github.pnoker.common.data.dal.MessageManager;
import io.github.pnoker.common.data.dal.NotifyChannelBindManager;
import io.github.pnoker.common.data.dal.NotifyChannelManager;
import io.github.pnoker.common.data.dal.NotifyManager;
import io.github.pnoker.common.data.dal.RuleManager;
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
    private RuleManager ruleManager;

    @Mock
    private NotifyBuilder notifyBuilder;

    @Mock
    private NotifyManager notifyManager;

    @Mock
    private MessageBuilder messageBuilder;

    @Mock
    private MessageManager messageManager;

    @Mock
    private NotifyChannelBuilder notifyChannelBuilder;

    @Mock
    private NotifyChannelManager notifyChannelManager;

    @Mock
    private NotifyChannelBindBuilder notifyChannelBindBuilder;

    @Mock
    private NotifyChannelBindManager notifyChannelBindManager;

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
        assertThatCode(() -> ruleService.selectByPage(new RuleQuery())).doesNotThrowAnyException();
        assertThatCode(() -> notifyService.selectByPage(new NotifyQuery())).doesNotThrowAnyException();
        assertThatCode(() -> messageService.selectByPage(new MessageQuery())).doesNotThrowAnyException();
        assertThatCode(() -> notifyChannelService.selectByPage(new NotifyChannelQuery())).doesNotThrowAnyException();
        assertThatCode(() -> notifyChannelBindService.selectByPage(new NotifyChannelBindQuery()))
                .doesNotThrowAnyException();
    }

}
