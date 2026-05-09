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

package io.github.pnoker.common.manager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.manager.entity.bo.*;
import io.github.pnoker.common.manager.entity.builder.DeviceBuilder;
import io.github.pnoker.common.manager.entity.builder.PointBuilder;
import io.github.pnoker.common.manager.entity.model.PointDataVolumeRunDO;
import io.github.pnoker.common.manager.entity.query.PointQuery;
import io.github.pnoker.common.manager.entity.vo.*;
import io.github.pnoker.common.manager.service.PointService;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerConstant.POINT_URL_PREFIX)
public class PointController implements BaseController {

    private final PointBuilder pointBuilder;

    private final PointService pointService;

    private final DeviceBuilder deviceBuilder;

    public PointController(PointBuilder pointBuilder, PointService pointService, DeviceBuilder deviceBuilder) {
        this.pointBuilder = pointBuilder;
        this.pointService = pointService;
        this.deviceBuilder = deviceBuilder;
    }

    /**
     * Point
     *
     * @param entityVO {@link PointVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<PointBO>> add(@Validated(Add.class) @RequestBody PointVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointBO entityBO = pointBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            pointService.save(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * ID Point
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable(value = "id") Long id) {
        return async(() -> {
            pointService.remove(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        });
    }

    /**
     * Point
     *
     * @param entityVO {@link PointVO}
     * @return R of String
     */
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody PointVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointBO entityBO = pointBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            pointService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * ID Point
     *
     * @param id ID
     * @return PointVO {@link PointVO}
     */
    @GetMapping("/id/{id}")
    public Mono<R<PointVO>> selectById(@NotNull @PathVariable(value = "id") Long id) {
        return async(() -> {
            PointBO entityBO = pointService.selectById(id);
            PointVO entityVO = pointBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        });
    }

    /**
     * ID Point
     *
     * @param pointIds Point ID
     * @return Map(ID, PointVO)
     */
    @PostMapping("/ids")
    public Mono<R<Map<Long, PointVO>>> selectByIds(@RequestBody Set<Long> pointIds) {
        return async(() -> {
            List<PointBO> entityBOList = pointService.selectByIds(pointIds);
            Map<Long, PointVO> deviceMap = entityBOList.stream()
                    .collect(Collectors.toMap(PointBO::getId, entityBO -> pointBuilder.buildVOByBO(entityBO)));
            return R.ok(deviceMap);
        });
    }

    /**
     * ID Point
     *
     * @param profileId Point ID
     * @return Point
     */
    @GetMapping("/profile_id/{profileId}")
    public Mono<R<List<PointVO>>> selectByProfileId(@NotNull @PathVariable(value = "profileId") Long profileId) {
        return async(() -> {
            List<PointBO> entityBOList = pointService.selectByProfileId(profileId);
            List<PointVO> entityVOList = pointBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        });
    }

    /**
     * Device ID Point
     *
     * @param deviceId Device ID
     * @return Point Array
     */
    @GetMapping("/device_id/{deviceId}")
    public Mono<R<List<PointVO>>> selectByDeviceId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        return async(() -> {
            List<PointBO> entityBOList = pointService.selectByDeviceId(deviceId);
            List<PointVO> entityVOList = pointBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        });
    }

    /**
     * Point
     *
     * @param entityQuery Point Dto
     * @return Page Of Point
     */
    @PostMapping("/list")
    public Mono<R<Page<PointVO>>> list(@RequestBody(required = false) PointQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointQuery query = Objects.isNull(entityQuery) ? new PointQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<PointBO> entityPageBO = pointService.selectByPage(query);
            Page<PointVO> entityPageVO = pointBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

    /**
     * @param pointIds Point ID
     * @return Map String:String
     */
    @PostMapping("/unit")
    public Mono<R<Map<Long, String>>> unit(@RequestBody Set<Long> pointIds) {
        return async(() -> {
            Map<Long, String> units = pointService.unit(pointIds);
            if (Objects.nonNull(units)) {
                Map<Long, String> unitCodeMap = units.entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                return R.ok(unitCodeMap);
            }
            return R.fail();
        });
    }

    /**
     * @param pointId id
     * @return {@link R}<{@link Set}<{@link Long}>>
     */
    @GetMapping("/selectPointStatisticsWithDevice/{pointId}")
    public Mono<R<DeviceByPointVO>> selectPointStatisticsWithDevice(
            @NotNull @PathVariable(value = "pointId") Long pointId) {
        return async(() -> {
            DeviceByPointBO deviceByPointBO = pointService.selectPointStatisticsWithDevice(pointId);
            DeviceByPointVO deviceByPointVO = deviceBuilder.buildVOPointByBO(deviceByPointBO);
            return R.ok(deviceByPointVO);
        });
    }

    /**
     *
     * id
     *
     * @param pointId   id
     * @param deviceIds id
     * @return {@link R}<{@link Map}<{@link Long}, {@link String}>>
     */
    @PostMapping("/selectPointStatisticsByDeviceId/{pointId}")
    public Mono<R<List<PointDataVolumeRunVO>>> selectPointStatisticsByDeviceId(
            @NotNull @PathVariable(value = "pointId") Long pointId, @NotNull @RequestBody Set<Long> deviceIds) {
        return async(() -> {
            List<PointDataVolumeRunBO> list = pointService.selectPointStatisticsByDeviceId(pointId, deviceIds);
            List<PointDataVolumeRunVO> pointDataVolumeRunVO = pointBuilder.buildVOPointDataByBO(list);
            return R.ok(pointDataVolumeRunVO);
        });
    }

    /**
     *
     * id
     *
     * @param pointId id
     * @return {@link R}<{@link Map}<{@link Long}, {@link String}>>
     */
    @GetMapping("/selectPointStatisticsByPointId/{pointId}")
    public Mono<R<Long>> selectPointStatisticsByPointId(@NotNull @PathVariable(value = "pointId") Long pointId) {
        return async(() -> {
            PointDataVolumeRunDO pointDataVolumeRunDO = pointService.selectPointStatisticsByPointId(pointId);
            return R.ok(Objects.isNull(pointDataVolumeRunDO.getTotal()) ? 0 : pointDataVolumeRunDO.getTotal());
        });
    }

    /**
     * @param deviceId
     * @return
     */
    @GetMapping("/selectPointByDeviceId/{deviceId}")
    public Mono<R<Long>> selectPointByDeviceId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        return async(() -> {
            Long count = pointService.selectPointByDeviceId(deviceId);
            return R.ok(count);
        });
    }

    /**
     * @param deviceId
     * @return
     */
    @GetMapping("/selectPointConfigByDeviceId/{deviceId}")
    public Mono<R<PointConfigByDeviceVO>> selectPointConfigByDeviceId(
            @NotNull @PathVariable(value = "deviceId") Long deviceId) {
        return async(() -> {
            PointConfigByDeviceBO pointConfigByDeviceBO = pointService.selectPointConfigByDeviceId(deviceId);
            PointConfigByDeviceVO pointConfigByDeviceVO = pointBuilder.buildVODeviceByBO(pointConfigByDeviceBO);
            return R.ok(pointConfigByDeviceVO);
        });
    }

    /**
     * @param deviceId
     * @param pointIds
     * @return
     */
    @PostMapping("/selectDeviceStatisticsByPointId/{deviceId}")
    public Mono<R<List<DeviceDataVolumeRunVO>>> selectDeviceStatisticsByPointId(
            @NotNull @PathVariable(value = "deviceId") Long deviceId, @NotNull @RequestBody Set<Long> pointIds) {
        return async(() -> {
            List<DeviceDataVolumeRunBO> list = pointService.selectDeviceStatisticsByPointId(deviceId, pointIds);
            List<DeviceDataVolumeRunVO> deviceDataVolumeRunVOList = pointBuilder.buildVODeviceDataByBO(list);
            return R.ok(deviceDataVolumeRunVOList);
        });
    }

    /**
     * @param driverId
     * @return
     */
    @GetMapping("/selectPointDataByDriverId/{driverId}")
    public Mono<R<Long>> selectPointDataByDriverId(@NotNull @PathVariable(value = "driverId") Long driverId) {
        return async(() -> {
            PointDataVolumeRunDO pointDataVolumeRunDO = pointService.selectPointDataByDriverId(driverId);
            return R.ok(Objects.isNull(pointDataVolumeRunDO.getTotal()) ? 0 : pointDataVolumeRunDO.getTotal());
        });
    }

    /**
     * @param driverId
     * @return
     */
    @GetMapping("/selectPointByDriverId/{driverId}")
    public Mono<R<Long>> selectPointByDriverId(@NotNull @PathVariable(value = "driverId") Long driverId) {
        return async(() -> {
            Long result = pointService.selectPointByDriverId(driverId);
            return R.ok(result);
        });
    }

    @GetMapping("/selectPointDataStatisticsByDriverId/{driverId}")
    public Mono<R<PointDataStatisticsByDriverIdVO>> selectPointDataStatisticsByDriverId(
            @NotNull @PathVariable(value = "driverId") Long driverId) {
        return async(() -> {
            PointDataStatisticsByDriverIdBO pointDataStatisticsByDriverIdBOList = pointService
                    .selectPointDataStatisticsByDriverId(driverId);
            PointDataStatisticsByDriverIdVO pointDataStatisticsByDriverIdVOList = pointBuilder
                    .buildVOPointDataDriverByBO(pointDataStatisticsByDriverIdBOList);
            return R.ok(pointDataStatisticsByDriverIdVOList);
        });
    }

}
