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

package io.github.pnoker.common.manager.grpc.server.driver;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcPointDTO;
import io.github.pnoker.api.common.GrpcR;
import io.github.pnoker.api.common.driver.GrpcPagePointDTO;
import io.github.pnoker.api.common.driver.GrpcPagePointQuery;
import io.github.pnoker.api.common.driver.GrpcPointQuery;
import io.github.pnoker.api.common.driver.GrpcRPagePointDTO;
import io.github.pnoker.api.common.driver.GrpcRPointDTO;
import io.github.pnoker.api.common.driver.PointApiGrpc;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.query.PointQuery;
import io.github.pnoker.common.manager.grpc.builder.GrpcPointBuilder;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.PointService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * gRPC server handling driver-to-manager point requests.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverPointServer extends PointApiGrpc.PointApiImplBase {

    private final GrpcPointBuilder grpcPointBuilder;

    private final PointService pointService;

    private final DriverService driverService;

    private final DeviceService deviceService;

    @Override
    public void listByPage(GrpcPagePointQuery request, StreamObserver<GrpcRPagePointDTO> responseObserver) {
        GrpcRPagePointDTO.Builder builder = GrpcRPagePointDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        PointQuery query = grpcPointBuilder.buildQueryByGrpcQuery(request);

        Page<PointBO> entityPage = selectDriverScopedPage(request, query);
        if (Objects.isNull(entityPage)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            GrpcPagePointDTO.Builder pageBuilder = GrpcPagePointDTO.newBuilder();
            GrpcPage.Builder page = GrpcPage.newBuilder();
            page.setCurrent(entityPage.getCurrent());
            page.setSize(entityPage.getSize());
            page.setPages(entityPage.getPages());
            page.setTotal(entityPage.getTotal());
            pageBuilder.setPage(page);

            List<GrpcPointDTO> entityGrpcDTOList = entityPage.getRecords()
                    .stream()
                    .map(grpcPointBuilder::buildGrpcDTOByBO)
                    .toList();
            pageBuilder.addAllData(entityGrpcDTOList);

            builder.setData(pageBuilder);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getById(GrpcPointQuery request, StreamObserver<GrpcRPointDTO> responseObserver) {
        GrpcRPointDTO.Builder builder = GrpcRPointDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        DriverBO driverBO = selectDriver(request.getDriverId());
        PointBO entityBO = selectPoint(request.getPointId());
        if (Objects.isNull(entityBO) || Objects.isNull(driverBO)
                || !Objects.equals(entityBO.getTenantId(), driverBO.getTenantId())
                || !driverHasPoint(driverBO, entityBO)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            builder.setData(grpcPointBuilder.buildGrpcDTOByBO(entityBO));
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private Page<PointBO> selectDriverScopedPage(GrpcPagePointQuery request, PointQuery query) {
        Page<PointBO> page = new Page<>(query.getPage().getCurrent(), query.getPage().getSize());
        DriverBO driverBO = selectDriver(request.getDriverId());
        if (Objects.isNull(driverBO) || !Objects.equals(query.getTenantId(), driverBO.getTenantId())) {
            return null;
        }

        Set<Long> profileIds = resolveDriverProfileIds(request, driverBO);
        if (profileIds.isEmpty()) {
            page.setRecords(Collections.emptyList());
            return page;
        }

        List<PointBO> points = pointService.selectByProfileIds(profileIds.stream().toList())
                .stream()
                .filter(point -> Objects.equals(driverBO.getTenantId(), point.getTenantId()))
                .filter(point -> request.getPointId() <= 0 || Objects.equals(point.getId(), request.getPointId()))
                .toList();

        long total = points.size();
        long current = page.getCurrent();
        long size = page.getSize();
        int from = (int) Math.min((current - 1) * size, total);
        int to = (int) Math.min(from + size, total);

        page.setTotal(total);
        page.setRecords(points.subList(from, to));
        return page;
    }

    private Set<Long> resolveDriverProfileIds(GrpcPagePointQuery request, DriverBO driverBO) {
        if (request.getDeviceId() > 0) {
            DeviceBO deviceBO = selectDevice(request.getDeviceId());
            if (Objects.isNull(deviceBO) || !Objects.equals(deviceBO.getDriverId(), driverBO.getId())
                    || !Objects.equals(deviceBO.getTenantId(), driverBO.getTenantId())) {
                return Collections.emptySet();
            }
            return filterProfileId(request, deviceBO.getProfileId());
        }

        Set<Long> profileIds = deviceService.listByDriverId(driverBO.getId(), driverBO.getTenantId())
                .stream()
                .filter(device -> Objects.equals(driverBO.getTenantId(), device.getTenantId()))
                .map(DeviceBO::getProfileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return filterProfileId(request, profileIds);
    }

    private Set<Long> filterProfileId(GrpcPagePointQuery request, Long profileId) {
        if (Objects.isNull(profileId)) {
            return Collections.emptySet();
        }
        if (request.getProfileId() <= 0) {
            return Set.of(profileId);
        }
        return Objects.equals(profileId, request.getProfileId()) ? Set.of(profileId) : Collections.emptySet();
    }

    private Set<Long> filterProfileId(GrpcPagePointQuery request, Set<Long> profileIds) {
        if (request.getProfileId() <= 0) {
            return profileIds;
        }
        return profileIds.contains(request.getProfileId()) ? Set.of(request.getProfileId()) : Collections.emptySet();
    }

    private boolean driverHasPoint(DriverBO driverBO, PointBO pointBO) {
        return deviceService.listByDriverId(driverBO.getId(), driverBO.getTenantId())
                .stream()
                .filter(device -> Objects.equals(driverBO.getTenantId(), device.getTenantId()))
                .map(DeviceBO::getProfileId)
                .filter(Objects::nonNull)
                .anyMatch(profileId -> Objects.equals(profileId, pointBO.getProfileId()));
    }

    private DriverBO selectDriver(Long driverId) {
        try {
            return driverService.getById(driverId);
        } catch (Exception e) {
            return null;
        }
    }

    private DeviceBO selectDevice(Long deviceId) {
        try {
            return deviceService.getById(deviceId);
        } catch (Exception e) {
            return null;
        }
    }

    private PointBO selectPoint(Long pointId) {
        try {
            return pointService.getById(pointId);
        } catch (Exception e) {
            return null;
        }
    }

}
