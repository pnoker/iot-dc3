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
package io.github.pnoker.common.facade.local;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.data.biz.CommandHistoryService;
import io.github.pnoker.common.data.entity.vo.CommandHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.CommandHistoryVO;
import io.github.pnoker.common.enums.CommandHistorySourceEnum;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandHistoryBO;
import io.github.pnoker.db.core.page.OffsetPage;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CommandHistoryLocalFacadeTest {

    @Mock
    private CommandHistoryService commandHistoryService;

    @Test
    void mapsRecordFieldsIntoTheFacadeBo() {
        CommandHistoryVO record = new CommandHistoryVO();
        record.setRecordId("rec-7");
        record.setDeviceId("101");
        record.setCommandId("202");
        record.setCommandCode("restart");
        record.setParamValues("{\"speed\":3}");
        record.setResultValues("{\"ok\":true}");
        record.setStatus(PointCommandStatusEnum.SUCCESS);
        record.setSource(CommandHistorySourceEnum.GRPC);
        record.setOccurTime(LocalDateTime.of(2026, 10, 7, 10, 0));
        record.setFinishTime(LocalDateTime.of(2026, 10, 7, 10, 0, 5));
        when(commandHistoryService.getByRecordId(11L, "rec-7")).thenReturn(Mono.just(record));

        FacadeCommandHistoryBO bo = new CommandHistoryLocalFacade(commandHistoryService)
                .getByRecordIdReactive(11L, "rec-7")
                .block();
        assertThat(bo.getRecordId()).isEqualTo("rec-7");
        assertThat(bo.getDeviceId()).isEqualTo("101");
        assertThat(bo.getStatus()).isEqualTo(PointCommandStatusEnum.SUCCESS.getCode());
        assertThat(bo.getOccurTime()).isEqualTo("2026-10-07T10:00");
        assertThat(bo.getFinishTime()).isEqualTo("2026-10-07T10:00:05");
    }

    @Test
    void filtersAndPaginatesThroughTheQueryVo() {
        when(commandHistoryService.list(
                        org.mockito.ArgumentMatchers.eq(11L),
                        argThat((CommandHistoryQueryVO q) -> q != null
                                && "101".equals(q.getDeviceId())
                                && q.getStatus() == PointCommandStatusEnum.FAILED
                                && q.getOffset() == 0
                                && q.getLimit() == 20)))
                .thenReturn(Mono.just(OffsetPage.<CommandHistoryVO>of(java.util.Collections.emptyList(), 0, 20, 0)));

        OffsetPage<FacadeCommandHistoryBO> page = new CommandHistoryLocalFacade(commandHistoryService)
                .listReactive(11L, "101", null, null, "failed", 0, 20)
                .block();
        assertThat(page.total()).isZero();
    }
}
