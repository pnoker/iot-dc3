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
import io.github.pnoker.center.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.center.manager.entity.builder.PointAttributeConfigBuilder;
import io.github.pnoker.center.manager.entity.query.PointAttributeConfigQuery;
import io.github.pnoker.center.manager.entity.vo.PointAttributeConfigVO;
import io.github.pnoker.center.manager.service.PointAttributeConfigService;
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
 * 位号属性配置信息 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@Tag(name = "接口-位号属性配置")
@RequestMapping(ManagerConstant.POINT_ATTRIBUTE_CONFIG_URL_PREFIX)
public class PointAttributeConfigController implements BaseController {

    @Resource
    private PointAttributeConfigBuilder pointAttributeConfigBuilder;

    @Resource
    private PointAttributeConfigService pointAttributeConfigService;

    /**
     * 新增 PointInfo
     *
     * @param entityVO {@link PointAttributeConfigVO}
     * @return R of String
     */
    @PostMapping("/add")
    @Operation(summary = "新增-位号属性配置")
    public R<String> add(@Validated(Add.class) @RequestBody PointAttributeConfigVO entityVO) {
        try {
            PointAttributeConfigBO entityBO = pointAttributeConfigBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(getTenantId());
            pointAttributeConfigService.save(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除 PointInfo
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            pointAttributeConfigService.remove(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新 PointInfo
     *
     * @param entityVO {@link PointAttributeConfigVO}
     * @return R of String
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody PointAttributeConfigVO entityVO) {
        try {
            PointAttributeConfigBO entityBO = pointAttributeConfigBuilder.buildBOByVO(entityVO);
            pointAttributeConfigService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 PointInfo
     *
     * @param id 位号信息ID
     * @return PointAttributeConfigVO {@link PointAttributeConfigVO}
     */
    @GetMapping("/id/{id}")
    public R<PointAttributeConfigVO> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            PointAttributeConfigBO entityBO = pointAttributeConfigService.selectById(id);
            PointAttributeConfigVO entityVO = pointAttributeConfigBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 属性ID、设备ID 和 位号ID 查询 PointInfo
     *
     * @param attributeId Attribute ID
     * @param deviceId    设备ID
     * @param pointId     位号ID
     * @return PointInfo
     */
    @GetMapping("/attribute_id/{attributeId}/device_id/{deviceId}/point_id/{pointId}")
    public R<PointAttributeConfigVO> selectByAttributeIdAndDeviceIdAndPointId(@NotNull @PathVariable(value = "attributeId") Long attributeId,
                                                                              @NotNull @PathVariable(value = "deviceId") Long deviceId,
                                                                              @NotNull @PathVariable(value = "pointId") Long pointId) {
        try {
            PointAttributeConfigBO entityBO = pointAttributeConfigService.selectByAttributeIdAndDeviceIdAndPointId(attributeId, deviceId, pointId);
            PointAttributeConfigVO entityVO = pointAttributeConfigBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 设备ID 和 位号ID 查询 PointInfo
     *
     * @param deviceId 设备ID
     * @param pointId  位号ID
     * @return PointInfo
     */
    @GetMapping("/device_id/{deviceId}/point_id/{pointId}")
    public R<List<PointAttributeConfigVO>> selectByDeviceIdAndPointId(@NotNull @PathVariable(value = "deviceId") Long deviceId,
                                                                      @NotNull @PathVariable(value = "pointId") Long pointId) {
        try {
            List<PointAttributeConfigBO> entityBOS = pointAttributeConfigService.selectByDeviceIdAndPointId(deviceId, pointId);
            List<PointAttributeConfigVO> entityVOS = pointAttributeConfigBuilder.buildVOListByBOList(entityBOS);
            return R.ok(entityVOS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 设备ID 查询 PointInfo
     *
     * @param deviceId 设备ID
     * @return PointInfo
     */
    @GetMapping("/device_id/{deviceId}")
    public R<List<PointAttributeConfigVO>> selectByDeviceId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        try {
            List<PointAttributeConfigBO> entityBOS = pointAttributeConfigService.selectByDeviceId(deviceId);
            List<PointAttributeConfigVO> entityVOS = pointAttributeConfigBuilder.buildVOListByBOList(entityBOS);
            return R.ok(entityVOS);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 分页查询 PointInfo
     *
     * @param entityQuery PointInfo Dto
     * @return Page Of PointInfo
     */
    @PostMapping("/list")
    public R<Page<PointAttributeConfigVO>> list(@RequestBody(required = false) PointAttributeConfigQuery entityQuery) {
        try {
            if (ObjectUtil.isEmpty(entityQuery)) {
                entityQuery = new PointAttributeConfigQuery();
            }
            entityQuery.setTenantId(getTenantId());
            Page<PointAttributeConfigBO> entityPageBO = pointAttributeConfigService.selectByPage(entityQuery);
            Page<PointAttributeConfigVO> entityPageVO = pointAttributeConfigBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

}
