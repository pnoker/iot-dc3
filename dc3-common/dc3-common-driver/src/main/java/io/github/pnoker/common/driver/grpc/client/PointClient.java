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
import io.github.pnoker.api.common.driver.GrpcPagePointDTO;
import io.github.pnoker.api.common.driver.GrpcPagePointQuery;
import io.github.pnoker.api.common.driver.GrpcPointQuery;
import io.github.pnoker.api.common.driver.GrpcRPagePointDTO;
import io.github.pnoker.api.common.driver.GrpcRPointDTO;
import io.github.pnoker.api.common.driver.PointApiGrpc;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.driver.entity.builder.PointBuilder;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * gRPC client used to query point metadata associated with the current driver.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class PointClient {

    private final PointApiGrpc.PointApiBlockingStub pointApiBlockingStub;

    private final DriverMetadata driverMetadata;

    private final PointBuilder pointBuilder;

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
     * Point ID
     *
     * @param id Point ID
     * @return PointDTO
     */
    public PointBO getById(Long id) {
        GrpcPointQuery.Builder query = GrpcPointQuery.newBuilder();
        query.setTenantId(driverMetadata.getDriver().getTenantId())
                .setDriverId(driverMetadata.getDriver().getId()).setPointId(id);
        GrpcRPointDTO rPointDTO = pointApiBlockingStub.getById(query.build());
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
        GrpcRPagePointDTO rPagePointDTO = pointApiBlockingStub.listByPage(query.build());
        if (!rPagePointDTO.getResult().getOk()) {
            throw new ServiceException("Failed to fetch point list");
        }
        return rPagePointDTO;
    }

}
