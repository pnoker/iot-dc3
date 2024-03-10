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
import io.github.pnoker.center.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.center.manager.entity.builder.DriverAttributeConfigBuilder;
import io.github.pnoker.center.manager.entity.query.DriverAttributeConfigQuery;
import io.github.pnoker.center.manager.entity.vo.DriverAttributeConfigVO;
import io.github.pnoker.center.manager.service.DriverAttributeConfigService;
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

/**
 * 驱动属性配置信息 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@Tag(name = "接口-驱动属性配置")
@RequestMapping(ManagerConstant.DRIVER_ATTRIBUTE_CONFIG_URL_PREFIX)
public class DriverAttributeConfigController implements BaseController {

    @Resource
    private DriverAttributeConfigBuilder driverAttributeConfigBuilder;

    @Resource
    private DriverAttributeConfigService driverAttributeConfigService;

    /**
     * 新增 DriverInfo
     *
     * @param entityVO {@link DriverAttributeConfigVO}
     * @return R of String
     */
    @PostMapping("/add")
    @Operation(summary = "新增-驱动属性配置")
    public R<String> add(@Validated(Add.class) @RequestBody DriverAttributeConfigVO entityVO) {
        try {
            DriverAttributeConfigBO entityBO = driverAttributeConfigBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(getTenantId());
            driverAttributeConfigService.save(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除 DriverInfo
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            driverAttributeConfigService.remove(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新 DriverInfo
     *
     * @param entityVO {@link DriverAttributeConfigVO}
     * @return R of String
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody DriverAttributeConfigVO entityVO) {
        try {
            DriverAttributeConfigBO entityBO = driverAttributeConfigBuilder.buildBOByVO(entityVO);
            driverAttributeConfigService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 DriverInfo
     *
     * @param id ID
     * @return DriverAttributeConfigVO {@link DriverAttributeConfigVO}
     */
    @GetMapping("/id/{id}")
    public R<DriverAttributeConfigVO> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            DriverAttributeConfigBO entityBO = driverAttributeConfigService.selectById(id);
            DriverAttributeConfigVO entityVO = driverAttributeConfigBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 属性ID 和 设备ID 查询 DriverInfo
     *
     * @param attributeId Attribute ID
     * @param deviceId    设备ID
     * @return DriverInfo
     */
    @GetMapping("/device_id/{deviceId}/attribute_id/{attributeId}")
    public R<DriverAttributeConfigVO> selectByDeviceIdAndAttributeId(@NotNull @PathVariable(value = "deviceId") Long deviceId,
                                                                     @NotNull @PathVariable(value = "attributeId") Long attributeId) {
        try {
            DriverAttributeConfigBO entityBO = driverAttributeConfigService.selectByAttributeIdAndDeviceId(deviceId, attributeId);
            DriverAttributeConfigVO entityVO = driverAttributeConfigBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 设备ID 查询 DriverInfo
     *
     * @param deviceId 设备ID
     * @return DriverInfo Array
     */
    @GetMapping("/device_id/{deviceId}")
    public R<List<DriverAttributeConfigVO>> selectByDeviceId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        try {
            List<DriverAttributeConfigBO> entityBOS = driverAttributeConfigService.selectByDeviceId(deviceId);
            List<DriverAttributeConfigVO> entityVOS = driverAttributeConfigBuilder.buildVOListByBOList(entityBOS);
            return R.ok(entityVOS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 分页查询 DriverInfo
     *
     * @param entityQuery DriverInfo Dto
     * @return Page Of DriverInfo
     */
    @PostMapping("/list")
    public R<Page<DriverAttributeConfigVO>> list(@RequestBody(required = false) DriverAttributeConfigQuery entityQuery) {
        try {
            if (ObjectUtil.isEmpty(entityQuery)) {
                entityQuery = new DriverAttributeConfigQuery();
            }
            entityQuery.setTenantId(getTenantId());
            Page<DriverAttributeConfigBO> entityPageBO = driverAttributeConfigService.selectByPage(entityQuery);
            Page<DriverAttributeConfigVO> entityPageVO = driverAttributeConfigBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

}
