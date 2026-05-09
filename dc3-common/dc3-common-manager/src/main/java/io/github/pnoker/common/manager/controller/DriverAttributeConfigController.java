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
import io.github.pnoker.common.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.common.manager.entity.builder.DriverAttributeConfigBuilder;
import io.github.pnoker.common.manager.entity.query.DriverAttributeConfigQuery;
import io.github.pnoker.common.manager.entity.vo.DriverAttributeConfigVO;
import io.github.pnoker.common.manager.service.DriverAttributeConfigService;
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
 * Controller
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerConstant.DRIVER_ATTRIBUTE_CONFIG_URL_PREFIX)
public class DriverAttributeConfigController implements BaseController {

    private final DriverAttributeConfigBuilder driverAttributeConfigBuilder;

    private final DriverAttributeConfigService driverAttributeConfigService;

    public DriverAttributeConfigController(DriverAttributeConfigBuilder driverAttributeConfigBuilder,
                                           DriverAttributeConfigService driverAttributeConfigService) {
        this.driverAttributeConfigBuilder = driverAttributeConfigBuilder;
        this.driverAttributeConfigService = driverAttributeConfigService;
    }

    /**
     * DriverConfig
     *
     * @param entityVO {@link DriverAttributeConfigVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody DriverAttributeConfigVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverAttributeConfigBO entityBO = driverAttributeConfigBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            driverAttributeConfigService.save(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    /**
     * ID DriverConfig
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable(value = "id") Long id) {
        return async(() -> {
            driverAttributeConfigService.remove(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        });
    }

    /**
     * DriverConfig
     *
     * @param entityVO {@link DriverAttributeConfigVO}
     * @return R of String
     */
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody DriverAttributeConfigVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverAttributeConfigBO entityBO = driverAttributeConfigBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            driverAttributeConfigService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    /**
     * ID DriverConfig
     *
     * @param id ID
     * @return DriverAttributeConfigVO {@link DriverAttributeConfigVO}
     */
    @GetMapping("/id/{id}")
    public Mono<R<DriverAttributeConfigVO>> selectById(@NotNull @PathVariable(value = "id") Long id) {
        return async(() -> {
            DriverAttributeConfigBO entityBO = driverAttributeConfigService.selectById(id);
            DriverAttributeConfigVO entityVO = driverAttributeConfigBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        });
    }

    /**
     * ID Device ID DriverConfig
     *
     * @param attributeId Attribute ID
     * @param deviceId    Device ID
     * @return DriverConfig
     */
    @GetMapping("/device_id/{deviceId}/attribute_id/{attributeId}")
    public Mono<R<DriverAttributeConfigVO>> selectByDeviceIdAndAttributeId(
            @NotNull @PathVariable(value = "deviceId") Long deviceId,
            @NotNull @PathVariable(value = "attributeId") Long attributeId) {
        return async(() -> {
            DriverAttributeConfigBO entityBO = driverAttributeConfigService.selectByAttributeIdAndDeviceId(deviceId,
                    attributeId);
            DriverAttributeConfigVO entityVO = driverAttributeConfigBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        });
    }

    /**
     * Device ID DriverConfig
     *
     * @param deviceId Device ID
     * @return DriverConfig
     */
    @GetMapping("/device_id/{deviceId}")
    public Mono<R<List<DriverAttributeConfigVO>>> selectByDeviceId(
            @NotNull @PathVariable(value = "deviceId") Long deviceId) {
        return async(() -> {
            List<DriverAttributeConfigBO> entityBOList = driverAttributeConfigService.selectByDeviceId(deviceId);
            List<DriverAttributeConfigVO> entityVOList = driverAttributeConfigBuilder.buildVOListByBOList(entityBOList);
            return R.ok(entityVOList);
        });
    }

    /**
     * DriverConfig
     *
     * @param entityQuery DriverConfig Dto
     * @return Page Of DriverConfig
     */
    @PostMapping("/list")
    public Mono<R<Page<DriverAttributeConfigVO>>> list(
            @RequestBody(required = false) DriverAttributeConfigQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            DriverAttributeConfigQuery query = Objects.isNull(entityQuery) ? new DriverAttributeConfigQuery()
                    : entityQuery;
            query.setTenantId(tenantId);
            Page<DriverAttributeConfigBO> entityPageBO = driverAttributeConfigService.selectByPage(query);
            Page<DriverAttributeConfigVO> entityPageVO = driverAttributeConfigBuilder
                    .buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        }));
    }

}
