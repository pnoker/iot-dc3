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
import io.github.pnoker.center.manager.entity.query.DriverAttributeConfigPageQuery;
import io.github.pnoker.center.manager.service.DriverInfoService;
import io.github.pnoker.center.manager.service.NotifyService;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.model.DriverAttributeConfig;
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 位号配置信息 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerServiceConstant.DRIVER_INFO_URL_PREFIX)
public class DriverInfoController {

    @Resource
    private DriverInfoService driverInfoService;

    @Resource
    private NotifyService notifyService;

    /**
     * 新增 DriverInfo
     *
     * @param driverAttributeConfig DriverInfo
     * @return DriverInfo
     */
    @PostMapping("/add")
    public R<DriverAttributeConfig> add(@Validated(Insert.class) @RequestBody DriverAttributeConfig driverAttributeConfig) {
        try {
            DriverAttributeConfig add = driverInfoService.add(driverAttributeConfig);
            if (ObjectUtil.isNotNull(add)) {
                notifyService.notifyDriverDriverInfo(MetadataCommandTypeEnum.ADD, add);
                return R.ok(add);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 根据 ID 删除 DriverInfo
     *
     * @param id 驱动信息ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    public R<Boolean> delete(@NotNull @PathVariable(value = "id") String id) {
        try {
            DriverAttributeConfig driverAttributeConfig = driverInfoService.selectById(id);
            if (ObjectUtil.isNotNull(driverAttributeConfig) && driverInfoService.delete(id)) {
                notifyService.notifyDriverDriverInfo(MetadataCommandTypeEnum.DELETE, driverAttributeConfig);
                return R.ok();
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 修改 DriverInfo
     *
     * @param driverAttributeConfig DriverInfo
     * @return DriverInfo
     */
    @PostMapping("/update")
    public R<DriverAttributeConfig> update(@Validated(Update.class) @RequestBody DriverAttributeConfig driverAttributeConfig) {
        try {
            DriverAttributeConfig update = driverInfoService.update(driverAttributeConfig);
            if (ObjectUtil.isNotNull(update)) {
                notifyService.notifyDriverDriverInfo(MetadataCommandTypeEnum.UPDATE, update);
                return R.ok(update);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 根据 ID 查询 DriverInfo
     *
     * @param id 驱动信息ID
     * @return DriverInfo
     */
    @GetMapping("/id/{id}")
    public R<DriverAttributeConfig> selectById(@NotNull @PathVariable(value = "id") String id) {
        try {
            DriverAttributeConfig select = driverInfoService.selectById(id);
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
    public R<DriverAttributeConfig> selectByDeviceIdAndAttributeId(@NotNull @PathVariable(value = "deviceId") String deviceId,
                                                                   @NotNull @PathVariable(value = "attributeId") String attributeId) {
        try {
            DriverAttributeConfig select = driverInfoService.selectByDeviceIdAndAttributeId(deviceId, attributeId);
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
    public R<List<DriverAttributeConfig>> selectByDeviceId(@NotNull @PathVariable(value = "deviceId") String deviceId) {
        try {
            List<DriverAttributeConfig> select = driverInfoService.selectByDeviceId(deviceId);
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 模糊分页查询 DriverInfo
     *
     * @param driverInfoPageQuery DriverInfo Dto
     * @return Page Of DriverInfo
     */
    @PostMapping("/list")
    public R<Page<DriverAttributeConfig>> list(@RequestBody(required = false) DriverAttributeConfigPageQuery driverInfoPageQuery) {
        try {
            if (ObjectUtil.isEmpty(driverInfoPageQuery)) {
                driverInfoPageQuery = new DriverAttributeConfigPageQuery();
            }
            Page<DriverAttributeConfig> page = driverInfoService.list(driverInfoPageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
