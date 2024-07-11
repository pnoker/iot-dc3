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
import io.github.pnoker.common.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.common.manager.entity.builder.PointAttributeConfigBuilder;
import io.github.pnoker.common.manager.entity.query.PointAttributeConfigQuery;
import io.github.pnoker.common.manager.entity.vo.PointAttributeConfigVO;
import io.github.pnoker.common.manager.service.PointAttributeConfigService;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * 位号属性配置信息 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerConstant.POINT_ATTRIBUTE_CONFIG_URL_PREFIX)
public class PointAttributeConfigController implements BaseController {

    private final PointAttributeConfigBuilder pointAttributeConfigBuilder;
    private final PointAttributeConfigService pointAttributeConfigService;

    public PointAttributeConfigController(PointAttributeConfigBuilder pointAttributeConfigBuilder, PointAttributeConfigService pointAttributeConfigService) {
        this.pointAttributeConfigBuilder = pointAttributeConfigBuilder;
        this.pointAttributeConfigService = pointAttributeConfigService;
    }

    /**
     * 新增 PointConfig
     *
     * @param entityVO {@link PointAttributeConfigVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody PointAttributeConfigVO entityVO) {
        try {
            PointAttributeConfigBO entityBO = pointAttributeConfigBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(getTenantId());
            pointAttributeConfigService.save(entityBO);
            return Mono.just(R.ok(ResponseEnum.ADD_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 删除 PointConfig
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            pointAttributeConfigService.remove(id);
            return Mono.just(R.ok(ResponseEnum.DELETE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 更新 PointConfig
     *
     * @param entityVO {@link PointAttributeConfigVO}
     * @return R of String
     */
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody PointAttributeConfigVO entityVO) {
        try {
            PointAttributeConfigBO entityBO = pointAttributeConfigBuilder.buildBOByVO(entityVO);
            pointAttributeConfigService.update(entityBO);
            return Mono.just(R.ok(ResponseEnum.UPDATE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 查询 PointConfig
     *
     * @param id 位号信息ID
     * @return PointAttributeConfigVO {@link PointAttributeConfigVO}
     */
    @GetMapping("/id/{id}")
    public Mono<R<PointAttributeConfigVO>> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            PointAttributeConfigBO entityBO = pointAttributeConfigService.selectById(id);
            PointAttributeConfigVO entityVO = pointAttributeConfigBuilder.buildVOByBO(entityBO);
            return Mono.just(R.ok(entityVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 属性ID, 设备ID 和 位号ID 查询 PointConfig
     *
     * @param attributeId Attribute ID
     * @param deviceId    设备ID
     * @param pointId     位号ID
     * @return PointConfig
     */
    @GetMapping("/attribute_id/{attributeId}/device_id/{deviceId}/point_id/{pointId}")
    public Mono<R<PointAttributeConfigVO>> selectByAttributeIdAndDeviceIdAndPointId(@NotNull @PathVariable(value = "attributeId") Long attributeId,
                                                                                    @NotNull @PathVariable(value = "deviceId") Long deviceId,
                                                                                    @NotNull @PathVariable(value = "pointId") Long pointId) {
        try {
            PointAttributeConfigBO entityBO = pointAttributeConfigService.selectByAttributeIdAndDeviceIdAndPointId(attributeId, deviceId, pointId);
            PointAttributeConfigVO entityVO = pointAttributeConfigBuilder.buildVOByBO(entityBO);
            return Mono.just(R.ok(entityVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 设备ID 和 位号ID 查询 PointConfig
     *
     * @param deviceId 设备ID
     * @param pointId  位号ID
     * @return PointConfig
     */
    @GetMapping("/device_id/{deviceId}/point_id/{pointId}")
    public Mono<R<List<PointAttributeConfigVO>>> selectByDeviceIdAndPointId(@NotNull @PathVariable(value = "deviceId") Long deviceId,
                                                                            @NotNull @PathVariable(value = "pointId") Long pointId) {
        try {
            List<PointAttributeConfigBO> entityBOList = pointAttributeConfigService.selectByDeviceIdAndPointId(deviceId, pointId);
            List<PointAttributeConfigVO> entityVOList = pointAttributeConfigBuilder.buildVOListByBOList(entityBOList);
            return Mono.just(R.ok(entityVOList));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 设备ID 查询 PointConfig
     *
     * @param deviceId 设备ID
     * @return PointConfig
     */
    @GetMapping("/device_id/{deviceId}")
    public Mono<R<List<PointAttributeConfigVO>>> selectByDeviceId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        try {
            List<PointAttributeConfigBO> entityBOList = pointAttributeConfigService.selectByDeviceId(deviceId);
            List<PointAttributeConfigVO> entityVOList = pointAttributeConfigBuilder.buildVOListByBOList(entityBOList);
            return Mono.just(R.ok(entityVOList));
        } catch (Exception e) {
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 分页查询 PointConfig
     *
     * @param entityQuery PointConfig Dto
     * @return Page Of PointConfig
     */
    @PostMapping("/list")
    public Mono<R<Page<PointAttributeConfigVO>>> list(@RequestBody(required = false) PointAttributeConfigQuery entityQuery) {
        try {
            if (Objects.isNull(entityQuery)) {
                entityQuery = new PointAttributeConfigQuery();
            }
            entityQuery.setTenantId(getTenantId());
            Page<PointAttributeConfigBO> entityPageBO = pointAttributeConfigService.selectByPage(entityQuery);
            Page<PointAttributeConfigVO> entityPageVO = pointAttributeConfigBuilder.buildVOPageByBOPage(entityPageBO);
            return Mono.just(R.ok(entityPageVO));
        } catch (Exception e) {
            return Mono.just(R.fail(e.getMessage()));
        }
    }

}
