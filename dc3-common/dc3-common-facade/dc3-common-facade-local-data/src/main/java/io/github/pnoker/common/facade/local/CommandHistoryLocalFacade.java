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

import io.github.pnoker.common.data.biz.CommandHistoryService;
import io.github.pnoker.common.data.entity.vo.CommandHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.CommandHistoryVO;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.facade.api.CommandHistoryFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandHistoryBO;
import io.github.pnoker.db.core.page.OffsetPage;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * In-process CommandHistoryFacade backed by {@link CommandHistoryService}.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandHistoryLocalFacade implements CommandHistoryFacade {

    private final CommandHistoryService commandHistoryService;

    @Override
    public Mono<FacadeCommandHistoryBO> getByRecordIdReactive(Long tenantId, String recordId) {
        return commandHistoryService.getByRecordId(tenantId, recordId).map(this::toFacadeBO);
    }

    @Override
    public Mono<OffsetPage<FacadeCommandHistoryBO>> listReactive(
            Long tenantId,
            String deviceId,
            String commandId,
            String commandCode,
            String status,
            long offset,
            int limit) {
        CommandHistoryQueryVO query = new CommandHistoryQueryVO();
        query.setDeviceId(blankToNull(deviceId));
        query.setCommandId(blankToNull(commandId));
        query.setCommandCode(blankToNull(commandCode));
        query.setStatus(statusOf(status));
        query.setOffset(Math.max(0, offset));
        query.setLimit(Math.max(1, limit));
        return commandHistoryService
                .list(tenantId, query)
                .map(page -> new OffsetPage<>(
                        page.items().stream().map(this::toFacadeBO).toList(),
                        page.offset(),
                        page.limit(),
                        page.total(),
                        page.hasNext()));
    }

    private FacadeCommandHistoryBO toFacadeBO(CommandHistoryVO record) {
        return new FacadeCommandHistoryBO(
                record.getRecordId(),
                record.getDeviceId(),
                record.getCommandId(),
                record.getCommandCode(),
                record.getParamValues(),
                record.getResultValues(),
                codeOf(record.getStatus()),
                record.getErrorCode(),
                record.getErrorMessage(),
                Objects.isNull(record.getSource()) ? null : record.getSource().getCode(),
                record.getSourceUserId(),
                iso(record.getOccurTime()),
                iso(record.getSendTime()),
                iso(record.getFinishTime()),
                iso(record.getExpireTime()));
    }

    private static PointCommandStatusEnum statusOf(String status) {
        if (Objects.isNull(status) || status.isBlank()) {
            return null;
        }
        return PointCommandStatusEnum.ofCode(status);
    }

    private static String codeOf(PointCommandStatusEnum status) {
        return Objects.isNull(status) ? null : status.getCode();
    }

    private static String blankToNull(String value) {
        return Objects.isNull(value) || value.isBlank() ? null : value;
    }

    private static String iso(java.time.LocalDateTime value) {
        return Objects.isNull(value) ? null : value.toString();
    }
}
