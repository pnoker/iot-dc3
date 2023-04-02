/*
 * Copyright 2016-present the original author or authors.
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
import io.github.pnoker.center.manager.entity.query.PointAttributeConfigPageQuery;
import io.github.pnoker.center.manager.service.PointAttributeConfigService;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.model.PointAttributeConfig;
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
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
@RequestMapping(ManagerServiceConstant.POINT_ATTRIBUTE_CONFIG_URL_PREFIX)
public class PointAttributeConfigController {

    @Resource
    private PointAttributeConfigService pointAttributeConfigService;

    /**
     * 新增 PointInfo
     *
     * @param pointAttributeConfig PointInfo
     * @return PointInfo
     */
    @PostMapping("/add")
    public R<String> add(@Validated(Insert.class) @RequestBody PointAttributeConfig pointAttributeConfig) {
        try {
            pointAttributeConfigService.add(pointAttributeConfig);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除 PointInfo
     *
     * @param id 位号信息ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") String id) {
        try {
            pointAttributeConfigService.delete(id);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 修改 PointInfo
     *
     * @param pointAttributeConfig PointInfo
     * @return PointInfo
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody PointAttributeConfig pointAttributeConfig) {
        try {
            pointAttributeConfigService.update(pointAttributeConfig);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 PointInfo
     *
     * @param id 位号信息ID
     * @return PointInfo
     */
    @GetMapping("/id/{id}")
    public R<PointAttributeConfig> selectById(@NotNull @PathVariable(value = "id") String id) {
        try {
            PointAttributeConfig select = pointAttributeConfigService.selectById(id);
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 根据 属性ID、设备ID 和 位号ID 查询 PointInfo
     *
     * @param attributeId Attribute ID
     * @param deviceId    设备ID
     * @param pointId     Point ID
     * @return PointInfo
     */
    @GetMapping("/attribute_id/{attributeId}/device_id/{deviceId}/point_id/{pointId}")
    public R<PointAttributeConfig> selectByAttributeIdAndDeviceIdAndPointId(@NotNull @PathVariable(value = "attributeId") String attributeId,
                                                                            @NotNull @PathVariable(value = "deviceId") String deviceId,
                                                                            @NotNull @PathVariable(value = "pointId") String pointId) {
        try {
            PointAttributeConfig select = pointAttributeConfigService.selectByAttributeIdAndDeviceIdAndPointId(attributeId, deviceId, pointId);
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 根据 设备ID 和 位号ID 查询 PointInfo
     *
     * @param deviceId 设备ID
     * @param pointId  位号ID
     * @return PointInfo
     */
    @GetMapping("/device_id/{deviceId}/point_id/{pointId}")
    public R<List<PointAttributeConfig>> selectByDeviceIdAndPointId(@NotNull @PathVariable(value = "deviceId") String deviceId,
                                                                    @NotNull @PathVariable(value = "pointId") String pointId) {
        try {
            List<PointAttributeConfig> select = pointAttributeConfigService.selectByDeviceIdAndPointId(deviceId, pointId);
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 根据 设备ID 查询 PointInfo
     *
     * @param deviceId 设备ID
     * @return PointInfo
     */
    @GetMapping("/device_id/{deviceId}")
    public R<List<PointAttributeConfig>> selectByDeviceId(@NotNull @PathVariable(value = "deviceId") String deviceId) {
        try {
            List<PointAttributeConfig> select = pointAttributeConfigService.selectByDeviceId(deviceId);
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 模糊分页查询 PointInfo
     *
     * @param pointInfoPageQuery PointInfo Dto
     * @return Page Of PointInfo
     */
    @PostMapping("/list")
    public R<Page<PointAttributeConfig>> list(@RequestBody(required = false) PointAttributeConfigPageQuery pointInfoPageQuery) {
        try {
            if (ObjectUtil.isEmpty(pointInfoPageQuery)) {
                pointInfoPageQuery = new PointAttributeConfigPageQuery();
            }
            Page<PointAttributeConfig> page = pointAttributeConfigService.list(pointInfoPageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
