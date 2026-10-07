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
package io.github.pnoker.common.facade.grpc;

import io.github.pnoker.api.center.data.CommandHistoryApiGrpc;
import io.github.pnoker.api.center.data.GrpcCommandHistoryDTO;
import io.github.pnoker.api.center.data.GrpcCommandHistoryOffsetPage;
import io.github.pnoker.api.center.data.GrpcCommandHistoryQuery;
import io.github.pnoker.api.center.data.GrpcStringQuery;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.facade.api.CommandHistoryFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandHistoryBO;
import io.github.pnoker.db.core.page.OffsetPage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/**
 * gRPC CommandHistoryFacade: forwards command history reads to Data Center via
 * {@link CommandHistoryApiGrpc}.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandHistoryGrpcFacade implements CommandHistoryFacade {

    private final CommandHistoryApiGrpc.CommandHistoryApiStub commandHistoryApiStub;

    private final GrpcFacadeSupport grpcFacadeSupport;

    private final ObjectMapper objectMapper;

    @Override
    public Mono<FacadeCommandHistoryBO> getByRecordIdReactive(Long tenantId, String recordId) {
        if (Objects.isNull(tenantId) || tenantId <= 0 || Objects.isNull(recordId) || recordId.isBlank()) {
            return Mono.empty();
        }
        GrpcStringQuery request = GrpcStringQuery.newBuilder()
                .setTenantId(tenantId)
                .setValue(recordId)
                .build();
        return ReactiveGrpcClientSupport.<GrpcStringQuery, GrpcCommandHistoryDTO>unary(
                        "CommandHistoryFacade.getByRecordId",
                        observer ->
                                grpcFacadeSupport.withDeadline(commandHistoryApiStub).getByRecordId(request, observer))
                .map(this::toFacadeBO);
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
        if (Objects.isNull(tenantId) || tenantId <= 0) {
            return Mono.empty();
        }
        GrpcCommandHistoryQuery.Builder builder = GrpcCommandHistoryQuery.newBuilder()
                .setTenantId(tenantId)
                .setPage(
                        io.github.pnoker.api.common.PageRequest.newBuilder()
                                .setOffset(Math.max(0, offset))
                                .setLimit(Math.max(1, limit)));
        if (Objects.nonNull(deviceId) && !deviceId.isBlank()) {
            builder.setDeviceId(Long.parseLong(deviceId.trim()));
        }
        if (Objects.nonNull(commandId) && !commandId.isBlank()) {
            builder.setCommandId(Long.parseLong(commandId.trim()));
        }
        if (Objects.nonNull(commandCode) && !commandCode.isBlank()) {
            builder.setCommandCode(commandCode.trim());
        }
        PointCommandStatusEnum statusCode = statusOf(status);
        if (Objects.nonNull(statusCode)) {
            builder.setStatus(statusCode.getIndex());
        }
        GrpcCommandHistoryQuery request = builder.build();
        return ReactiveGrpcClientSupport.<GrpcCommandHistoryQuery, GrpcCommandHistoryOffsetPage>unary(
                        "CommandHistoryFacade.list",
                        observer -> grpcFacadeSupport.withDeadline(commandHistoryApiStub).list(request, observer))
                .map(this::toFacadePage);
    }

    private OffsetPage<FacadeCommandHistoryBO> toFacadePage(GrpcCommandHistoryOffsetPage page) {
        return new OffsetPage<>(
                page.getItemsList().stream().map(this::toFacadeBO).toList(),
                page.getPage().getOffset(),
                page.getPage().getLimit(),
                page.getPage().getTotal(),
                page.getPage().getHasNext());
    }

    private FacadeCommandHistoryBO toFacadeBO(GrpcCommandHistoryDTO dto) {
        return new FacadeCommandHistoryBO(
                text(dto.getRecordId()),
                dto.getDeviceId() > 0 ? String.valueOf(dto.getDeviceId()) : null,
                dto.getCommandId() > 0 ? String.valueOf(dto.getCommandId()) : null,
                text(dto.getCommandCode()),
                json(dto.getParamValuesMap()),
                json(dto.getResultValuesMap()),
                dto.getStatus() > 0 ? PointCommandStatusEnum.ofIndex((byte) dto.getStatus()).getCode() : null,
                text(dto.getErrorCode()),
                text(dto.getErrorMessage()),
                dto.getSource() > 0 ? String.valueOf(dto.getSource()) : null,
                dto.getSourceUserId() > 0 ? String.valueOf(dto.getSourceUserId()) : null,
                iso(dto.getOccurTime()),
                iso(dto.getSendTime()),
                iso(dto.getFinishTime()),
                iso(dto.getExpireTime()));
    }

    private static PointCommandStatusEnum statusOf(String status) {
        if (Objects.isNull(status) || status.isBlank()) {
            return null;
        }
        return PointCommandStatusEnum.ofCode(status.trim());
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String iso(long epochSecond) {
        if (epochSecond <= 0) {
            return null;
        }
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC);
        return time.toString();
    }

    private String json(java.util.Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return objectMapper.writeValueAsString(new TreeMap<>(values));
    }
}
