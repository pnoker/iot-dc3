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
 * 位号 Controller
 *
 * @author pnoker
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
     * 新增 Point
     *
     * @param entityVO {@link PointVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<PointBO>> add(@Validated(Add.class) @RequestBody PointVO entityVO) {
        try {
            PointBO entityBO = pointBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(getTenantId());
            pointService.save(entityBO);
            return Mono.just(R.ok(ResponseEnum.ADD_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 删除 Point
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            pointService.remove(id);
            return Mono.just(R.ok(ResponseEnum.DELETE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 更新 Point
     *
     * @param entityVO {@link PointVO}
     * @return R of String
     */
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody PointVO entityVO) {
        try {
            PointBO entityBO = pointBuilder.buildBOByVO(entityVO);
            pointService.update(entityBO);
            return Mono.just(R.ok(ResponseEnum.UPDATE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 查询 Point
     *
     * @param id ID
     * @return PointVO {@link PointVO}
     */
    @GetMapping("/id/{id}")
    public Mono<R<PointVO>> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            PointBO entityBO = pointService.selectById(id);
            PointVO entityVO = pointBuilder.buildVOByBO(entityBO);
            return Mono.just(R.ok(entityVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 集合查询 Point
     *
     * @param pointIds 位号ID集
     * @return Map(ID, PointVO)
     */
    @PostMapping("/ids")
    public Mono<R<Map<Long, PointVO>>> selectByIds(@RequestBody Set<Long> pointIds) {
        try {
            List<PointBO> entityBOList = pointService.selectByIds(pointIds);
            Map<Long, PointVO> deviceMap = entityBOList.stream().collect(Collectors.toMap(PointBO::getId, entityBO -> pointBuilder.buildVOByBO(entityBO)));
            return Mono.just(R.ok(deviceMap));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 模版 ID 查询 Point
     *
     * @param profileId 位号ID
     * @return Point 集合
     */
    @GetMapping("/profile_id/{profileId}")
    public Mono<R<List<PointVO>>> selectByProfileId(@NotNull @PathVariable(value = "profileId") Long profileId) {
        try {
            List<PointBO> entityBOList = pointService.selectByProfileId(profileId);
            List<PointVO> entityVOList = pointBuilder.buildVOListByBOList(entityBOList);
            return Mono.just(R.ok(entityVOList));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 设备ID 查询 Point
     *
     * @param deviceId 设备ID
     * @return Point Array
     */
    @GetMapping("/device_id/{deviceId}")
    public Mono<R<List<PointVO>>> selectByDeviceId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        try {
            List<PointBO> entityBOList = pointService.selectByDeviceId(deviceId);
            List<PointVO> entityVOList = pointBuilder.buildVOListByBOList(entityBOList);
            return Mono.just(R.ok(entityVOList));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 分页查询 Point
     *
     * @param entityQuery Point Dto
     * @return Page Of Point
     */
    @PostMapping("/list")
    public Mono<R<Page<PointVO>>> list(@RequestBody(required = false) PointQuery entityQuery) {
        try {
            if (Objects.isNull(entityQuery)) {
                entityQuery = new PointQuery();
            }
            entityQuery.setTenantId(getTenantId());
            Page<PointBO> entityPageBO = pointService.selectByPage(entityQuery);
            Page<PointVO> entityPageVO = pointBuilder.buildVOPageByBOPage(entityPageBO);
            return Mono.just(R.ok(entityPageVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 查询 位号单位
     *
     * @param pointIds 位号ID集
     * @return Map String:String
     */
    @PostMapping("/unit")
    public Mono<R<Map<Long, String>>> unit(@RequestBody Set<Long> pointIds) {
        try {
            Map<Long, String> units = pointService.unit(pointIds);
            if (Objects.nonNull(units)) {
                Map<Long, String> unitCodeMap = units.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                return Mono.just(R.ok(unitCodeMap));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
        return Mono.just(R.fail());
    }


    /**
     * 查询位号被多少设备引用
     * 选择点位统计设备信息
     *
     * @param pointId 点位id
     * @return {@link R}<{@link Set}<{@link Long}>>
     */
    @GetMapping("/selectPointStatisticsWithDevice/{pointId}")
    public Mono<R<DeviceByPointVO>> selectPointStatisticsWithDevice(@NotNull @PathVariable(value = "pointId") Long pointId) {
        try {
            DeviceByPointBO deviceByPointBO = pointService.selectPointStatisticsWithDevice(pointId);
            DeviceByPointVO deviceByPointVO = deviceBuilder.buildVOPointByBO(deviceByPointBO);
            return Mono.just(R.ok(deviceByPointVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }


    /**
     * 查询位号在不同设备下的数据量
     * 按设备id统计位号数量
     *
     * @param pointId   点位id
     * @param deviceIds 设备id
     * @return {@link R}<{@link Map}<{@link Long}, {@link String}>>
     */
    @PostMapping("/selectPointStatisticsByDeviceId/{pointId}")
    public Mono<R<List<PointDataVolumeRunVO>>> selectPointStatisticsByDeviceId(@NotNull @PathVariable(value = "pointId") Long pointId, @NotNull @RequestBody Set<Long> deviceIds) {
        try {
            List<PointDataVolumeRunBO> list = pointService.selectPointStatisticsByDeviceId(pointId, deviceIds);
            List<PointDataVolumeRunVO> pointDataVolumeRunVO = pointBuilder.buildVOPointDataByBO(list);
            return Mono.just(R.ok(pointDataVolumeRunVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 当前位号下数据量
     * 按位号id统计位号数据量
     *
     * @param pointId 点位id
     * @return {@link R}<{@link Map}<{@link Long}, {@link String}>>
     */
    @GetMapping("/selectPointStatisticsByPointId/{pointId}")
    public Mono<R<Long>> selectPointStatisticsByPointId(@NotNull @PathVariable(value = "pointId") Long pointId) {
        try {
            PointDataVolumeRunDO pointDataVolumeRunDO = pointService.selectPointStatisticsByPointId(pointId);
            return Mono.just(R.ok(Objects.isNull(pointDataVolumeRunDO.getTotal()) ? 0 : pointDataVolumeRunDO.getTotal()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 设备下位号数量
     *
     * @param deviceId
     * @return
     */
    @GetMapping("/selectPointByDeviceId/{deviceId}")
    public Mono<R<Long>> selectPointByDeviceId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        try {
            Long count = pointService.selectPointByDeviceId(deviceId);
            return Mono.just(R.ok(count));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 设备下已配置位号数量 下拉框
     *
     * @param deviceId
     * @return
     */
    @GetMapping("/selectPointConfigByDeviceId/{deviceId}")
    public Mono<R<PointConfigByDeviceVO>> selectPointConfigByDeviceId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        try {
            PointConfigByDeviceBO pointConfigByDeviceBO = pointService.selectPointConfigByDeviceId(deviceId);
            PointConfigByDeviceVO pointConfigByDeviceVO = pointBuilder.buildVODeviceByBO(pointConfigByDeviceBO);
            return Mono.just(R.ok(pointConfigByDeviceVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }


    /**
     * 位号在不同位号下的数据量
     *
     * @param deviceId
     * @param pointIds
     * @return
     */
    @PostMapping("/selectDeviceStatisticsByPointId/{deviceId}")
    public Mono<R<List<DeviceDataVolumeRunVO>>> selectDeviceStatisticsByPointId(@NotNull @PathVariable(value = "deviceId") Long deviceId, @NotNull @RequestBody Set<Long> pointIds) {
        try {
            List<DeviceDataVolumeRunBO> list = pointService.selectDeviceStatisticsByPointId(deviceId, pointIds);
            List<DeviceDataVolumeRunVO> deviceDataVolumeRunVOList = pointBuilder.buildVODeviceDataByBO(list);
            return Mono.just(R.ok(deviceDataVolumeRunVOList));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 驱动下位号数据量
     *
     * @param driverId
     * @return
     */
    @GetMapping("/selectPointDataByDriverId/{driverId}")
    public Mono<R<Long>> selectPointDataByDriverId(@NotNull @PathVariable(value = "driverId") Long driverId) {
        try {
            PointDataVolumeRunDO pointDataVolumeRunDO = pointService.selectPointDataByDriverId(driverId);
            return Mono.just(R.ok(Objects.isNull(pointDataVolumeRunDO.getTotal()) ? 0 : pointDataVolumeRunDO.getTotal()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 驱动下位号数量
     *
     * @param driverId
     * @return
     */
    @GetMapping("/selectPointByDriverId/{driverId}")
    public Mono<R<Long>> selectPointByDriverId(@NotNull @PathVariable(value = "driverId") Long driverId) {
        try {
            Long result = pointService.selectPointByDriverId(driverId);
            return Mono.just(R.ok(result));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @GetMapping("/selectPointDataStatisticsByDriverId/{driverId}")
    public Mono<R<PointDataStatisticsByDriverIdVO>> selectPointDataStatisticsByDriverId(@NotNull @PathVariable(value = "driverId") Long driverId) {
        try {
            PointDataStatisticsByDriverIdBO pointDataStatisticsByDriverIdBOList = pointService.selectPointDataStatisticsByDriverId(driverId);
            PointDataStatisticsByDriverIdVO pointDataStatisticsByDriverIdVOList = pointBuilder.buildVOPointDataDriverByBO(pointDataStatisticsByDriverIdBOList);
            return Mono.just(R.ok(pointDataStatisticsByDriverIdVOList));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

}
