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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.bo.PointBO;
import io.github.pnoker.center.manager.entity.query.PointQuery;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.common.base.Controller;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 位号 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerConstant.POINT_URL_PREFIX)
public class PointController implements Controller {

    @Resource
    private PointService pointService;

    /**
     * 新增 Point
     *
     * @param pointBO Point
     * @return Point
     */
    @PostMapping("/add")
    public R<PointBO> add(@Validated(Add.class) @RequestBody PointBO pointBO) {
        try {
            pointBO.setTenantId(getTenantId());
            pointService.save(pointBO);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除 Point
     *
     * @param id 位号ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") String id) {
        try {
            pointService.remove(Long.parseLong(id));
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新 Point
     *
     * @param pointBO Point
     * @return Point
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody PointBO pointBO) {
        try {
            pointBO.setTenantId(getTenantId());
            pointService.update(pointBO);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 Point
     *
     * @param id 位号ID
     * @return Point
     */
    @GetMapping("/id/{id}")
    public R<PointBO> selectById(@NotNull @PathVariable(value = "id") String id) {
        try {
            PointBO select = pointService.selectById(Long.parseLong(id));
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 根据 ID 集合查询 Point
     *
     * @param pointIds 位号ID集
     * @return Map String:Point
     */
    @PostMapping("/ids")
    public R<Map<Long, PointBO>> selectByIds(@RequestBody Set<Long> pointIds) {
        try {
            List<PointBO> pointBOS = pointService.selectByIds(pointIds);
            Map<Long, PointBO> pointMap = pointBOS.stream().collect(Collectors.toMap(PointBO::getId, Function.identity()));
            return R.ok(pointMap);
            // todo 返回id为 long，前端无法解析
        } catch (Exception e) {
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
    public R<List<PointBO>> selectByProfileId(@NotNull @PathVariable(value = "profileId") Long profileId) {
        try {
            List<PointBO> select = pointService.selectByProfileId(profileId);
            if (CollUtil.isNotEmpty(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 根据 设备ID 查询 Point
     *
     * @param deviceId 设备ID
     * @return Point Array
     */
    @GetMapping("/device_id/{deviceId}")
    public R<List<PointBO>> selectByDeviceId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        try {
            List<PointBO> select = pointService.selectByDeviceId(deviceId);
            if (CollUtil.isNotEmpty(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 分页查询 Point
     *
     * @param pointPageQuery Point Dto
     * @return Page Of Point
     */
    @PostMapping("/list")
    public R<Page<PointBO>> list(@RequestBody(required = false) PointQuery pointPageQuery) {
        try {
            if (ObjectUtil.isEmpty(pointPageQuery)) {
                pointPageQuery = new PointQuery();
            }
            Page<PointBO> page = pointService.selectByPage(pointPageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
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
            // todo 返回id为 long，前端无法解析
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
