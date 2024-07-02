/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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

package io.github.pnoker.common.driver.grpc.client;

import io.github.pnoker.api.common.GrpcDriverAttributeConfigDTO;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcPointAttributeConfigDTO;
import io.github.pnoker.api.common.driver.*;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.builder.DeviceBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcDriverAttributeConfigBuilder;
import io.github.pnoker.common.driver.entity.builder.GrpcPointAttributeConfigBuilder;
import io.github.pnoker.common.driver.entity.dto.DriverAttributeConfigDTO;
import io.github.pnoker.common.driver.entity.dto.PointAttributeConfigDTO;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.optional.CollectionOptional;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DeviceClient {

    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private DeviceApiGrpc.DeviceApiBlockingStub deviceApiBlockingStub;

    @Resource
    private DriverMetadata driverMetadata;

    @Resource
    private DeviceBuilder deviceBuilder;
    @Resource
    private GrpcDriverAttributeConfigBuilder grpcDriverAttributeConfigBuilder;
    @Resource
    private GrpcPointAttributeConfigBuilder grpcPointAttributeConfigBuilder;

    public List<DeviceBO> list() {
        long current = 1;
        GrpcRPageDeviceDTO rPageDeviceDTO = getGrpcRPageDeviceDTO(current);
        GrpcPageDeviceDTO pageDTO = rPageDeviceDTO.getData();
        List<GrpcRDeviceAttachDTO> dataList = pageDTO.getDataList();
        List<DeviceBO> deviceBOList = dataList.stream().map(this::buildDTOByGrpcAttachDTO).toList();
        List<DeviceBO> allDeviceBOList = new ArrayList<>(deviceBOList);

        long pages = pageDTO.getPage().getPages();
        while (current < pages) {
            current++;
            GrpcRPageDeviceDTO tPageDeviceDTO = getGrpcRPageDeviceDTO(current);
            GrpcPageDeviceDTO tPageDTO = tPageDeviceDTO.getData();
            List<GrpcRDeviceAttachDTO> tDataList = tPageDTO.getDataList();
            List<DeviceBO> tDeviceBOList = tDataList.stream().map(this::buildDTOByGrpcAttachDTO).toList();
            allDeviceBOList.addAll(tDeviceBOList);
            pages = tPageDTO.getPage().getPages();
        }
        return allDeviceBOList;
    }

    /**
     * 根据 设备ID 获取设备元数据
     *
     * @param id 设备ID
     * @return DeviceBO
     */
    public DeviceBO selectById(Long id) {
        GrpcDeviceQuery.Builder query = GrpcDeviceQuery.newBuilder();
        query.setDeviceId(id);
        GrpcRDeviceDTO rDeviceDTO = deviceApiBlockingStub.selectById(query.build());
        if (!rDeviceDTO.getResult().getOk()) {
            log.error("Device doesn't exist: {}", id);
            return null;
        }

        GrpcRDeviceAttachDTO rDeviceAttachDTO = rDeviceDTO.getData();
        return buildDTOByGrpcAttachDTO(rDeviceAttachDTO);
    }

    private GrpcRPageDeviceDTO getGrpcRPageDeviceDTO(long current) {
        GrpcPageDeviceQuery.Builder query = GrpcPageDeviceQuery.newBuilder();
        GrpcPage.Builder page = GrpcPage.newBuilder();
        page.setCurrent(current);
        query.setTenantId(driverMetadata.getDriver().getTenantId())
                .setDriverId(driverMetadata.getDriver().getId())
                .setPage(page);
        GrpcRPageDeviceDTO rPageDeviceDTO = deviceApiBlockingStub.selectByPage(query.build());
        if (!rPageDeviceDTO.getResult().getOk()) {
            throw new ServiceException("获取设备列表失败");
        }
        return rPageDeviceDTO;
    }

    private DeviceBO buildDTOByGrpcAttachDTO(GrpcRDeviceAttachDTO rDeviceAttachDTO) {
        DeviceBO deviceBO = deviceBuilder.buildDTOByGrpcDTO(rDeviceAttachDTO.getDevice());
        deviceBO.setPointIds(new HashSet<>(rDeviceAttachDTO.getPointIdsList()));

        CollectionOptional.ofNullable(rDeviceAttachDTO.getDriverConfigsList()).ifPresent(value -> {
            Map<Long, DriverAttributeConfigDTO> driverAttributeConfigMap = value.stream()
                    .collect(Collectors.toMap(GrpcDriverAttributeConfigDTO::getDriverAttributeId, grpcDriverAttributeConfigBuilder::buildDTOByGrpcDTO));
            deviceBO.setDriverAttributeConfigIdMap(driverAttributeConfigMap);
        });

        CollectionOptional.ofNullable(rDeviceAttachDTO.getPointConfigsList()).ifPresent(value -> {
            Map<Long, Map<Long, PointAttributeConfigDTO>> pointAttributeConfigMap = value.stream()
                    .collect(Collectors.groupingBy(GrpcPointAttributeConfigDTO::getPointId, Collectors.toMap(GrpcPointAttributeConfigDTO::getPointAttributeId, grpcPointAttributeConfigBuilder::buildDTOByGrpcDTO)));
            deviceBO.setPointAttributeConfigIdMap(pointAttributeConfigMap);
        });

        return deviceBO;
    }
}
