/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.manager.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.PointInfoPageQuery;
import io.github.pnoker.center.manager.service.NotifyService;
import io.github.pnoker.center.manager.service.PointInfoService;
import io.github.pnoker.common.constant.driver.MetadataConstant;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.model.PointInfo;
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
@RequestMapping(ManagerServiceConstant.POINT_INFO_URL_PREFIX)
public class PointInfoController {

    @Resource
    private PointInfoService pointInfoService;

    @Resource
    private NotifyService notifyService;

    /**
     * 新增 PointInfo
     *
     * @param pointInfo PointInfo
     * @return PointInfo
     */
    @PostMapping("/add")
    public R<PointInfo> add(@Validated(Insert.class) @RequestBody PointInfo pointInfo) {
        try {
            PointInfo add = pointInfoService.add(pointInfo);
            if (ObjectUtil.isNotNull(add)) {
                notifyService.notifyDriverPointInfo(MetadataConstant.PointInfo.ADD, add);
                return R.ok(add);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 根据 ID 删除 PointInfo
     *
     * @param id 位号信息ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    public R<Boolean> delete(@NotNull @PathVariable(value = "id") String id) {
        try {
            PointInfo pointInfo = pointInfoService.selectById(id);
            if (ObjectUtil.isNotNull(pointInfo) && pointInfoService.delete(id)) {
                notifyService.notifyDriverPointInfo(MetadataConstant.PointInfo.DELETE, pointInfo);
                return R.ok();
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 修改 PointInfo
     *
     * @param pointInfo PointInfo
     * @return PointInfo
     */
    @PostMapping("/update")
    public R<PointInfo> update(@Validated(Update.class) @RequestBody PointInfo pointInfo) {
        try {
            PointInfo update = pointInfoService.update(pointInfo);
            if (null != update) {
                notifyService.notifyDriverPointInfo(MetadataConstant.PointInfo.UPDATE, update);
                return R.ok(update);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 根据 ID 查询 PointInfo
     *
     * @param id 位号信息ID
     * @return PointInfo
     */
    @GetMapping("/id/{id}")
    public R<PointInfo> selectById(@NotNull @PathVariable(value = "id") String id) {
        try {
            PointInfo select = pointInfoService.selectById(id);
            if (null != select) {
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
    public R<PointInfo> selectByAttributeIdAndDeviceIdAndPointId(@NotNull @PathVariable(value = "attributeId") String attributeId,
                                                                 @NotNull @PathVariable(value = "deviceId") String deviceId,
                                                                 @NotNull @PathVariable(value = "pointId") String pointId) {
        try {
            PointInfo select = pointInfoService.selectByAttributeIdAndDeviceIdAndPointId(attributeId, deviceId, pointId);
            if (null != select) {
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
    public R<List<PointInfo>> selectByDeviceIdAndPointId(@NotNull @PathVariable(value = "deviceId") String deviceId,
                                                         @NotNull @PathVariable(value = "pointId") String pointId) {
        try {
            List<PointInfo> select = pointInfoService.selectByDeviceIdAndPointId(deviceId, pointId);
            if (null != select) {
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
    public R<List<PointInfo>> selectByDeviceId(@NotNull @PathVariable(value = "deviceId") String deviceId) {
        try {
            List<PointInfo> select = pointInfoService.selectByDeviceId(deviceId);
            if (null != select) {
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
    public R<Page<PointInfo>> list(@RequestBody(required = false) PointInfoPageQuery pointInfoPageQuery) {
        try {
            if (ObjectUtil.isEmpty(pointInfoPageQuery)) {
                pointInfoPageQuery = new PointInfoPageQuery();
            }
            Page<PointInfo> page = pointInfoService.list(pointInfoPageQuery);
            if (null != page) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
