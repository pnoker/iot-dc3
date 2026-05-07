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

import io.github.pnoker.api.center.data.GrpcPointValueCommandQuery;
import io.github.pnoker.api.center.data.GrpcPointValueWriteCommand;
import io.github.pnoker.api.center.data.GrpcRBoolean;
import io.github.pnoker.api.center.data.PointValueApiGrpc;
import io.github.pnoker.common.facade.api.PointValueCommandFacade;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * gRPC implementation: forwards command calls to Data Center via
 * {@link PointValueApiGrpc.PointValueApiBlockingStub}.
 * <p>
 * Selected when {@code dc3.facade.mode=grpc} (or unset — grpc is the default in the
 * auto-configuration declaration).
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.5
 */
@Slf4j
@Component
public class PointValueCommandGrpcFacade implements PointValueCommandFacade {

    @Resource
    private PointValueApiGrpc.PointValueApiBlockingStub pointValueApiBlockingStub;

    @Override
    public boolean read(Long deviceId, Long pointId) {
        GrpcPointValueCommandQuery request = GrpcPointValueCommandQuery.newBuilder()
                .setDeviceId(deviceId)
                .setPointId(pointId)
                .build();
        GrpcRBoolean response = pointValueApiBlockingStub.readCommand(request);
        return response.getResult().getOk() && response.getData();
    }

    @Override
    public boolean write(Long deviceId, Long pointId, String value) {
        GrpcPointValueWriteCommand request = GrpcPointValueWriteCommand.newBuilder()
                .setDeviceId(deviceId)
                .setPointId(pointId)
                .setValue(value)
                .build();
        GrpcRBoolean response = pointValueApiBlockingStub.writeCommand(request);
        return response.getResult().getOk() && response.getData();
    }

}
