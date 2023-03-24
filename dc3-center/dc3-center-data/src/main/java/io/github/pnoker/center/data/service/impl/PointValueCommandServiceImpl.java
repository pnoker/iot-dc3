/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.data.service.impl;

import io.github.pnoker.api.center.manager.ByDeviceQueryDTO;
import io.github.pnoker.api.center.manager.DriverApiGrpc;
import io.github.pnoker.api.center.manager.PointApiGrpc;
import io.github.pnoker.api.center.manager.RDriverDTO;
import io.github.pnoker.center.data.entity.vo.PointValueReadVO;
import io.github.pnoker.center.data.entity.vo.PointValueWriteVO;
import io.github.pnoker.center.data.service.PointValueCommandService;
import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.dto.DeviceCommandDTO;
import io.github.pnoker.common.enums.DeviceCommandTypeEnum;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class PointValueCommandServiceImpl implements PointValueCommandService {

    @GrpcClient(ManagerServiceConstant.SERVICE_NAME)
    private PointApiGrpc.PointApiBlockingStub pointApiBlockingStub;
    @GrpcClient(ManagerServiceConstant.SERVICE_NAME)
    private DriverApiGrpc.DriverApiBlockingStub driverApiBlockingStub;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public void read(PointValueReadVO entityVO) {
        ByDeviceQueryDTO.Builder builder = ByDeviceQueryDTO.newBuilder();
        builder.setDeviceId(entityVO.getDeviceId());
        RDriverDTO rDriverDTO = driverApiBlockingStub.selectByDeviceId(builder.build());
        if (!rDriverDTO.getResult().getOk()) {
            return;
        }

        DeviceCommandDTO.DeviceRead deviceRead = new DeviceCommandDTO.DeviceRead(entityVO.getDeviceId(), entityVO.getPointId());
        DeviceCommandDTO deviceCommandDTO = new DeviceCommandDTO(DeviceCommandTypeEnum.READ, JsonUtil.toJsonString(deviceRead));
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_COMMAND, RabbitConstant.ROUTING_DEVICE_COMMAND_PREFIX + rDriverDTO.getData().getServiceName(), deviceCommandDTO);
    }

    @Override
    public void write(PointValueWriteVO entityVO) {
        ByDeviceQueryDTO.Builder builder = ByDeviceQueryDTO.newBuilder();
        builder.setDeviceId(entityVO.getDeviceId());
        RDriverDTO rDriverDTO = driverApiBlockingStub.selectByDeviceId(builder.build());
        if (!rDriverDTO.getResult().getOk()) {
            return;
        }

        DeviceCommandDTO.DeviceWrite deviceWrite = new DeviceCommandDTO.DeviceWrite(entityVO.getDeviceId(), entityVO.getPointId(), entityVO.getValue());
        DeviceCommandDTO deviceCommandDTO = new DeviceCommandDTO(DeviceCommandTypeEnum.WRITE, JsonUtil.toJsonString(deviceWrite));
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_COMMAND, RabbitConstant.ROUTING_DEVICE_COMMAND_PREFIX + rDriverDTO.getData().getServiceName(), deviceCommandDTO);
    }
}
