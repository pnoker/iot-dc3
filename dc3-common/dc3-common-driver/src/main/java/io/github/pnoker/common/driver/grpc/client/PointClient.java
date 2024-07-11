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

import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcPointDTO;
import io.github.pnoker.api.common.driver.*;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.entity.builder.PointBuilder;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.exception.ServiceException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class PointClient {

    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private PointApiGrpc.PointApiBlockingStub pointApiBlockingStub;

    @Resource
    private DriverMetadata driverMetadata;

    @Resource
    private PointBuilder pointBuilder;

    public List<PointBO> list() {
        long current = 1;
        GrpcRPagePointDTO rPagePointDTO = getGrpcRPagePointDTO(current);
        GrpcPagePointDTO pageDTO = rPagePointDTO.getData();
        List<GrpcPointDTO> dataList = pageDTO.getDataList();
        List<PointBO> pointBOList = dataList.stream().map(pointBuilder::buildDTOByGrpcDTO).toList();
        ArrayList<PointBO> allPointBOList = new ArrayList<>(pointBOList);

        long pages = pageDTO.getPage().getPages();
        while (current < pages) {
            current++;
            GrpcRPagePointDTO tPagePointDTO = getGrpcRPagePointDTO(current);
            GrpcPagePointDTO tPageDTO = tPagePointDTO.getData();
            List<GrpcPointDTO> tDataList = tPageDTO.getDataList();
            List<PointBO> tPointBOList = tDataList.stream().map(pointBuilder::buildDTOByGrpcDTO).toList();
            allPointBOList.addAll(tPointBOList);
            pages = tPageDTO.getPage().getPages();
        }
        return allPointBOList;
    }

    /**
     * 根据 位号ID 获取位号元数据
     *
     * @param id 位号ID
     * @return PointDTO
     */
    public PointBO selectById(Long id) {
        GrpcPointQuery.Builder query = GrpcPointQuery.newBuilder();
        query.setPointId(id);
        GrpcRPointDTO rPointDTO = pointApiBlockingStub.selectById(query.build());
        if (!rPointDTO.getResult().getOk()) {
            log.error("Point doesn't exist: {}", id);
            return null;
        }

        return pointBuilder.buildDTOByGrpcDTO(rPointDTO.getData());
    }

    private GrpcRPagePointDTO getGrpcRPagePointDTO(long current) {
        GrpcPagePointQuery.Builder query = GrpcPagePointQuery.newBuilder();
        GrpcPage.Builder page = GrpcPage.newBuilder();
        page.setCurrent(current);
        query.setTenantId(driverMetadata.getDriver().getTenantId())
                .setDriverId(driverMetadata.getDriver().getId())
                .setPage(page);
        GrpcRPagePointDTO rPagePointDTO = pointApiBlockingStub.selectByPage(query.build());
        if (!rPagePointDTO.getResult().getOk()) {
            throw new ServiceException("获取设备列表失败");
        }
        return rPagePointDTO;
    }
}
