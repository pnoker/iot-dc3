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
import io.github.pnoker.common.manager.entity.bo.DeviceByPointBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.bo.PointConfigByDeviceBO;
import io.github.pnoker.common.manager.entity.builder.DeviceBuilder;
import io.github.pnoker.common.manager.entity.builder.PointBuilder;
import io.github.pnoker.common.manager.entity.query.PointQuery;
import io.github.pnoker.common.manager.entity.vo.DeviceByPointVO;
import io.github.pnoker.common.manager.entity.vo.PointConfigByDeviceVO;
import io.github.pnoker.common.manager.entity.vo.PointVO;
import io.github.pnoker.common.manager.service.DeviceService;
import io.github.pnoker.common.manager.service.PointService;
import io.github.pnoker.common.manager.service.ProfileService;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

    private final DeviceService deviceService;

    private final ProfileService profileService;

    public PointController(PointBuilder pointBuilder, PointService pointService, DeviceBuilder deviceBuilder,
                           DeviceService deviceService, ProfileService profileService) {
        this.pointBuilder = pointBuilder;
        this.pointService = pointService;
        this.deviceBuilder = deviceBuilder;
        this.deviceService = deviceService;
        this.profileService = profileService;
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
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, pointService.selectById(id));
            pointService.remove(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
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
            requireTenant(tenantId, pointService.selectById(entityBO.getId()));
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
        return getTenantId().flatMap(tenantId -> async(() -> {
            PointBO entityBO = requireTenant(tenantId, pointService.selectById(id));
            PointVO entityVO = pointBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        }));
    }

    /**
     * ID Point
     *
     * @param pointIds Point ID
     * @return Map(ID, PointVO)
     */
    @PostMapping("/ids")
    public Mono<R<Map<Long, PointVO>>> selectByIds(@RequestBody Set<Long> pointIds) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            List<PointBO> entityBOList = filterTenant(tenantId, pointService.selectByIds(pointIds));
            Map<Long, PointVO> deviceMap = entityBOList.stream()
                    .collect(Collectors.toMap(PointBO::getId, entityBO -> pointBuilder.buildVOByBO(entityBO)));
            return R.ok(deviceMap);
        }));
    }

    /**
     * ID Point
     *
     * @param profileId Point ID
     * @return Point
     */
    @GetMapping("/profile_id/{profileId}")
    public Mono<R<List<PointVO>>> selectByProfileId(@NotNull @PathVariable(value = "profileId") Long profileId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, profileService.selectById(profileId));
            List<PointBO> entityBOList = filterTenant(tenantId, pointService.selectByProfileId(profileId));
            List<PointVO> entityVOList = pointBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
    }

    /**
     * Device ID Point
     *
     * @param deviceId Device ID
     * @return Point Array
     */
    @GetMapping("/device_id/{deviceId}")
    public Mono<R<List<PointVO>>> selectByDeviceId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, deviceService.selectById(deviceId));
            List<PointBO> entityBOList = filterTenant(tenantId, pointService.selectByDeviceId(deviceId));
            List<PointVO> entityVOList = pointBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        }));
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
        return getTenantId().flatMap(tenantId -> async(() -> {
            Set<Long> scopedPointIds = filterTenant(tenantId, pointService.selectByIds(pointIds)).stream()
                    .map(PointBO::getId)
                    .collect(Collectors.toSet());
            Map<Long, String> units = pointService.unit(scopedPointIds);
            if (Objects.nonNull(units)) {
                Map<Long, String> unitCodeMap = units.entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                return R.ok(unitCodeMap);
            }
            return R.fail();
        }));
    }

    /**
     * @param pointId id
     * @return {@link R}<{@link Set}<{@link Long}>>
     */
    @GetMapping("/selectPointStatisticsWithDevice/{pointId}")
    public Mono<R<DeviceByPointVO>> selectPointStatisticsWithDevice(
            @NotNull @PathVariable(value = "pointId") Long pointId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, pointService.selectById(pointId));
            DeviceByPointBO deviceByPointBO = pointService.selectPointStatisticsWithDevice(pointId);
            DeviceByPointVO deviceByPointVO = deviceBuilder.buildVOPointByBO(deviceByPointBO);
            return R.ok(deviceByPointVO);
        }));
    }

    /**
     * @param deviceId
     * @return
     */
    @GetMapping("/selectPointByDeviceId/{deviceId}")
    public Mono<R<Long>> selectPointByDeviceId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, deviceService.selectById(deviceId));
            Long count = pointService.selectPointByDeviceId(deviceId);
            return R.ok(count);
        }));
    }

    /**
     * @param deviceId
     * @return
     */
    @GetMapping("/selectPointConfigByDeviceId/{deviceId}")
    public Mono<R<PointConfigByDeviceVO>> selectPointConfigByDeviceId(
            @NotNull @PathVariable(value = "deviceId") Long deviceId) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, deviceService.selectById(deviceId));
            PointConfigByDeviceBO pointConfigByDeviceBO = pointService.selectPointConfigByDeviceId(deviceId);
            PointConfigByDeviceVO pointConfigByDeviceVO = pointBuilder.buildVODeviceByBO(pointConfigByDeviceBO);
            return R.ok(pointConfigByDeviceVO);
        }));
    }

}
