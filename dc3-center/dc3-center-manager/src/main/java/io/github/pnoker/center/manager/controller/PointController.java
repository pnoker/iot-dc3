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

package io.github.pnoker.center.manager.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.bo.*;
import io.github.pnoker.center.manager.entity.builder.DeviceBuilder;
import io.github.pnoker.center.manager.entity.builder.PointBuilder;
import io.github.pnoker.center.manager.entity.model.PointDataVolumeRunDO;
import io.github.pnoker.center.manager.entity.query.PointQuery;
import io.github.pnoker.center.manager.entity.vo.*;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
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
@Tag(name = "接口-位号")
@RequestMapping(ManagerConstant.POINT_URL_PREFIX)
public class PointController implements BaseController {

    @Resource
    private PointBuilder pointBuilder;

    @Resource
    private PointService pointService;

    @Resource
    private DeviceBuilder deviceBuilder;


    /**
     * 新增 Point
     *
     * @param entityVO {@link PointVO}
     * @return R of String
     */
    @PostMapping("/add")
    @Operation(summary = "新增-位号")
    public R<PointBO> add(@Validated(Add.class) @RequestBody PointVO entityVO) {
        try {
            PointBO entityBO = pointBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(getTenantId());
            pointService.save(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除 Point
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            pointService.remove(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新 Point
     *
     * @param entityVO {@link PointVO}
     * @return R of String
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody PointVO entityVO) {
        try {
            PointBO entityBO = pointBuilder.buildBOByVO(entityVO);
            pointService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 Point
     *
     * @param id ID
     * @return PointVO {@link PointVO}
     */
    @GetMapping("/id/{id}")
    public R<PointVO> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            PointBO entityBO = pointService.selectById(id);
            PointVO entityVO = pointBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 集合查询 Point
     *
     * @param pointIds 位号ID集
     * @return Map(ID, PointVO)
     */
    @PostMapping("/ids")
    public R<Map<Long, PointVO>> selectByIds(@RequestBody Set<Long> pointIds) {
        try {
            List<PointBO> entityBOS = pointService.selectByIds(pointIds);
            Map<Long, PointVO> deviceMap = entityBOS.stream().collect(Collectors.toMap(PointBO::getId, entityBO -> pointBuilder.buildVOByBO(entityBO)));
            return R.ok(deviceMap);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 模板 ID 查询 Point
     *
     * @param profileId 位号ID
     * @return Point Array
     */
    @GetMapping("/profile_id/{profileId}")
    public R<List<PointVO>> selectByProfileId(@NotNull @PathVariable(value = "profileId") Long profileId) {
        try {
            List<PointBO> entityBOS = pointService.selectByProfileId(profileId);
            List<PointVO> entityVOS = pointBuilder.buildVOListByBOList(entityBOS);
            return R.ok(entityVOS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 设备ID 查询 Point
     *
     * @param deviceId 设备ID
     * @return Point Array
     */
    @GetMapping("/device_id/{deviceId}")
    public R<List<PointVO>> selectByDeviceId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        try {
            List<PointBO> entityBOS = pointService.selectByDeviceId(deviceId);
            List<PointVO> entityVOS = pointBuilder.buildVOListByBOList(entityBOS);
            return R.ok(entityVOS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 分页查询 Point
     *
     * @param entityQuery Point Dto
     * @return Page Of Point
     */
    @PostMapping("/list")
    public R<Page<PointVO>> list(@RequestBody(required = false) PointQuery entityQuery) {
        try {
            if (ObjectUtil.isEmpty(entityQuery)) {
                entityQuery = new PointQuery();
            }
            entityQuery.setTenantId(getTenantId());
            Page<PointBO> entityPageBO = pointService.selectByPage(entityQuery);
            Page<PointVO> entityPageVO = pointBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 查询 位号单位
     *
     * @param pointIds 位号ID集
     * @return Map String:String
     */
    @PostMapping("/unit")
    public R<Map<Long, String>> unit(@RequestBody Set<Long> pointIds) {
        try {
            Map<Long, String> units = pointService.unit(pointIds);
            if (ObjectUtil.isNotNull(units)) {
                Map<Long, String> unitCodeMap = units.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                return R.ok(unitCodeMap);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }


    /**
     * 查询位号被多少设备引用
     * 选择点位统计设备信息
     *
     * @param pointId 点位id
     * @return {@link R}<{@link Set}<{@link Long}>>
     */
    @GetMapping("/selectPointStatisticsWithDevice/{pointId}")
    @Operation(summary = "查询-位号被多少设备引用")
    public R<DeviceByPointVO> selectPointStatisticsWithDevice(@NotNull @PathVariable(value = "pointId") Long pointId) {
        try {
            DeviceByPointBO deviceByPointBO = pointService.selectPointStatisticsWithDevice(pointId);
            DeviceByPointVO deviceByPointVO = deviceBuilder.buildVOPointByBO(deviceByPointBO);
            return R.ok(deviceByPointVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
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
    @Operation(summary = "查询-位号在不同设备下的数据量")
    public R<List<PointDataVolumeRunVO>> selectPointStatisticsByDeviceId(@NotNull @PathVariable(value = "pointId") Long pointId, @NotNull @RequestBody Set<Long> deviceIds) {
        try {
            List<PointDataVolumeRunBO> list = pointService.selectPointStatisticsByDeviceId(pointId, deviceIds);
            List<PointDataVolumeRunVO> pointDataVolumeRunVO = pointBuilder.buildVOPointDataByBO(list);
            return R.ok(pointDataVolumeRunVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
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
    @Operation(summary = "查询-当前位号数据总量")
    public R<Long> selectPointStatisticsByPointId(@NotNull @PathVariable(value = "pointId") Long pointId) {
        try {
            PointDataVolumeRunDO pointDataVolumeRunDO = pointService.selectPointStatisticsByPointId(pointId);
            return R.ok(ObjectUtil.isEmpty(pointDataVolumeRunDO.getTotal()) ? 0 : pointDataVolumeRunDO.getTotal());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 设备下位号数量
     *
     * @param deviceId
     * @return
     */
    @GetMapping("/selectPointByDeviceId/{deviceId}")
    @Operation(summary = "查询-设备下位号数量")
    public R<Long> selectPointByDeviceId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        try {
            Long count = pointService.selectPointByDeviceId(deviceId);
            return R.ok(count);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 设备下已配置位号数量 下拉框
     *
     * @param deviceId
     * @return
     */
    @GetMapping("/selectPointConfigByDeviceId/{deviceId}")
    @Operation(summary = "查询-设备下已配置位号数量 下拉框")
    public R<PointConfigByDeviceVO> selectPointConfigByDeviceId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        try {
            PointConfigByDeviceBO pointConfigByDeviceBO = pointService.selectPointConfigByDeviceId(deviceId);
            PointConfigByDeviceVO pointConfigByDeviceVO = pointBuilder.buildVODeviceByBO(pointConfigByDeviceBO);
            return R.ok(pointConfigByDeviceVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
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
    @Operation(summary = "查询-设备在不同位号下的数据量")
    public R<List<DeviceDataVolumeRunVO>> selectDeviceStatisticsByPointId(@NotNull @PathVariable(value = "deviceId") Long deviceId, @NotNull @RequestBody Set<Long> pointIds) {
        try {
            List<DeviceDataVolumeRunBO> list = pointService.selectDeviceStatisticsByPointId(deviceId, pointIds);
            List<DeviceDataVolumeRunVO> deviceDataVolumeRunVOS = pointBuilder.buildVODeviceDataByBO(list);
            return R.ok(deviceDataVolumeRunVOS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 驱动下位号数据量
     *
     * @param driverId
     * @return
     */
    @GetMapping("/selectPointDataByDriverId/{driverId}")
    @Operation(summary = "查询-驱动下位号数据量")
    public R<Long> selectPointDataByDriverId(@NotNull @PathVariable(value = "driverId") Long driverId) {
        try {
            PointDataVolumeRunDO pointDataVolumeRunDO = pointService.selectPointDataByDriverId(driverId);
            return R.ok(ObjectUtil.isEmpty(pointDataVolumeRunDO.getTotal()) ? 0 : pointDataVolumeRunDO.getTotal());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 驱动下位号数量
     *
     * @param driverId
     * @return
     */
    @GetMapping("/selectPointByDriverId/{driverId}")
    @Operation(summary = "查询-驱动下位号数量")
    public R<Long> selectPointByDriverId(@NotNull @PathVariable(value = "driverId") Long driverId) {
        try {
            Long result = pointService.selectPointByDriverId(driverId);
            return R.ok(result);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/selectPointDataStatisticsByDriverId/{driverId}")
    @Operation(summary = "查询-统计7天驱动下位号数据量")
    public R<PointDataStatisticsByDriverIdVO> selectPointDataStatisticsByDriverId(@NotNull @PathVariable(value = "driverId") Long driverId) {
        try {
            PointDataStatisticsByDriverIdBO pointDataStatisticsByDriverIdBOS = pointService.selectPointDataStatisticsByDriverId(driverId);
            PointDataStatisticsByDriverIdVO pointDataStatisticsByDriverIdVOS = pointBuilder.buildVOPointDataDriverByBO(pointDataStatisticsByDriverIdBOS);
            return R.ok(pointDataStatisticsByDriverIdVOS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

}
