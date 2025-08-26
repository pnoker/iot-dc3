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

package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.api.center.manager.DriverApiGrpc;
import io.github.pnoker.api.center.manager.GrpcDeviceQuery;
import io.github.pnoker.api.center.manager.GrpcRDriverDTO;
import io.github.pnoker.api.center.manager.PointApiGrpc;
import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.data.biz.PointValueCommandService;
import io.github.pnoker.common.data.entity.vo.PointValueReadVO;
import io.github.pnoker.common.data.entity.vo.PointValueWriteVO;
import io.github.pnoker.common.entity.dto.DeviceCommandDTO;
import io.github.pnoker.common.enums.DeviceCommandTypeEnum;
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class PointValueCommandServiceImpl implements PointValueCommandService {

    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private PointApiGrpc.PointApiBlockingStub pointApiBlockingStub;
    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private DriverApiGrpc.DriverApiBlockingStub driverApiBlockingStub;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public void read(PointValueReadVO entityVO) {
        GrpcDeviceQuery.Builder builder = GrpcDeviceQuery.newBuilder();
        builder.setDeviceId(entityVO.getDeviceId());
        GrpcRDriverDTO rDriverDTO = driverApiBlockingStub.selectByDeviceId(builder.build());
        if (!rDriverDTO.getResult().getOk()) {
            return;
        }

        DeviceCommandDTO.DeviceRead deviceRead = new DeviceCommandDTO.DeviceRead(entityVO.getDeviceId(), entityVO.getPointId());
        DeviceCommandDTO deviceCommandDTO = new DeviceCommandDTO(DeviceCommandTypeEnum.READ, JsonUtil.toJsonString(deviceRead));
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_COMMAND, RabbitConstant.ROUTING_DEVICE_COMMAND_PREFIX + rDriverDTO.getData().getServiceName(), deviceCommandDTO);
    }

    @Override
    public void write(PointValueWriteVO entityVO) {
        GrpcDeviceQuery.Builder builder = GrpcDeviceQuery.newBuilder();
        builder.setDeviceId(entityVO.getDeviceId());
        GrpcRDriverDTO rDriverDTO = driverApiBlockingStub.selectByDeviceId(builder.build());
        if (!rDriverDTO.getResult().getOk()) {
            return;
        }

        DeviceCommandDTO.DeviceWrite deviceWrite = new DeviceCommandDTO.DeviceWrite(entityVO.getDeviceId(), entityVO.getPointId(), entityVO.getValue());
        DeviceCommandDTO deviceCommandDTO = new DeviceCommandDTO(DeviceCommandTypeEnum.WRITE, JsonUtil.toJsonString(deviceWrite));
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_COMMAND, RabbitConstant.ROUTING_DEVICE_COMMAND_PREFIX + rDriverDTO.getData().getServiceName(), deviceCommandDTO);
    }
}
