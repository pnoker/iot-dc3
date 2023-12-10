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
import io.github.pnoker.center.manager.entity.query.DriverAttributeConfigQuery;
import io.github.pnoker.center.manager.service.DriverAttributeConfigService;
import io.github.pnoker.common.base.Controller;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
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
@RequestMapping(ManagerServiceConstant.DRIVER_ATTRIBUTE_CONFIG_URL_PREFIX)
public class DriverAttributeConfigController implements Controller {

    @Resource
    private DriverAttributeConfigService driverAttributeConfigService;

    /**
     * 新增 DriverInfo
     *
     * @param driverAttributeConfigBO DriverInfo
     * @return DriverInfo
     */
    @PostMapping("/add")
    public R<String> add(@Validated(Add.class) @RequestBody DriverAttributeConfigBO driverAttributeConfigBO) {
        try {
            driverAttributeConfigService.save(driverAttributeConfigBO);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除 DriverInfo
     *
     * @param id 驱动信息ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") String id) {
        try {
            driverAttributeConfigService.remove(Long.parseLong(id));
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新 DriverInfo
     *
     * @param driverAttributeConfigBO DriverInfo
     * @return DriverInfo
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody DriverAttributeConfigBO driverAttributeConfigBO) {
        try {
            driverAttributeConfigService.update(driverAttributeConfigBO);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 DriverInfo
     *
     * @param id 驱动信息ID
     * @return DriverInfo
     */
    @GetMapping("/id/{id}")
    public R<DriverAttributeConfigBO> selectById(@NotNull @PathVariable(value = "id") String id) {
        try {
            DriverAttributeConfigBO select = driverAttributeConfigService.selectById(Long.parseLong(id));
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 根据 属性ID 和 设备ID 查询 DriverInfo
     *
     * @param attributeId Attribute ID
     * @param deviceId    设备ID
     * @return DriverInfo
     */
    @GetMapping("/device_id/{deviceId}/attribute_id/{attributeId}")
    public R<DriverAttributeConfigBO> selectByDeviceIdAndAttributeId(@NotNull @PathVariable(value = "deviceId") String deviceId,
                                                                     @NotNull @PathVariable(value = "attributeId") String attributeId) {
        try {
            DriverAttributeConfigBO select = driverAttributeConfigService.selectByAttributeIdAndDeviceId(Long.parseLong(deviceId), Long.parseLong(attributeId));
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 根据 设备ID 查询 DriverInfo
     *
     * @param deviceId 设备ID
     * @return DriverInfo Array
     */
    @GetMapping("/device_id/{deviceId}")
    public R<List<DriverAttributeConfigBO>> selectByDeviceId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        try {
            List<DriverAttributeConfigBO> select = driverAttributeConfigService.selectByDeviceId(deviceId);
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 分页查询 DriverInfo
     *
     * @param driverInfoPageQuery DriverInfo Dto
     * @return Page Of DriverInfo
     */
    @PostMapping("/list")
    public R<Page<DriverAttributeConfigBO>> list(@RequestBody(required = false) DriverAttributeConfigQuery driverInfoPageQuery) {
        try {
            if (ObjectUtil.isEmpty(driverInfoPageQuery)) {
                driverInfoPageQuery = new DriverAttributeConfigQuery();
            }
            Page<DriverAttributeConfigBO> page = driverAttributeConfigService.selectByPage(driverInfoPageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
