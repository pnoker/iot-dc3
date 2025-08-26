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
