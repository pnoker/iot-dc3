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
import io.github.pnoker.center.manager.entity.bo.PointBO;
import io.github.pnoker.center.manager.entity.builder.PointBuilder;
import io.github.pnoker.center.manager.entity.query.PointQuery;
import io.github.pnoker.center.manager.entity.vo.PointVO;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.enums.ResponseEnum;
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
public class PointController implements BaseController {

    @Resource
    private PointBuilder pointBuilder;

    @Resource
    private PointService pointService;

    /**
     * 新增 Point
     *
     * @param entityVO {@link PointVO}
     * @return R of String
     */
    @PostMapping("/add")
    public R<PointBO> add(@Validated(Add.class) @RequestBody PointVO entityVO) {
        try {
            PointBO entityBO = pointBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(getTenantId());
            pointService.save(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除 Point
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            pointService.remove(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新 Point
     *
     * @param entityVO {@link PointVO}
     * @return R of String
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody PointVO entityVO) {
        try {
            PointBO entityBO = pointBuilder.buildBOByVO(entityVO);
            pointService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 Point
     *
     * @param id ID
     * @return PointVO {@link PointVO}
     */
    @GetMapping("/id/{id}")
    public R<PointVO> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            PointBO entityBO = pointService.selectById(id);
            PointVO entityVO = pointBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 集合查询 Point
     *
     * @param pointIds 位号ID集
     * @return Map(ID, PointVO)
     */
    @PostMapping("/ids")
    public R<Map<Long, PointVO>> selectByIds(@RequestBody Set<Long> pointIds) {
        try {
            List<PointBO> entityBOS = pointService.selectByIds(pointIds);
            Map<Long, PointVO> deviceMap = entityBOS.stream().collect(Collectors.toMap(PointBO::getId, entityBO -> pointBuilder.buildVOByBO(entityBO)));
            return R.ok(deviceMap);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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
    public R<List<PointVO>> selectByProfileId(@NotNull @PathVariable(value = "profileId") Long profileId) {
        try {
            List<PointBO> entityBOS = pointService.selectByProfileId(profileId);
            List<PointVO> entityVOS = pointBuilder.buildVOListByBOList(entityBOS);
            return R.ok(entityVOS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 设备ID 查询 Point
     *
     * @param deviceId 设备ID
     * @return Point Array
     */
    @GetMapping("/device_id/{deviceId}")
    public R<List<PointVO>> selectByDeviceId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        try {
            List<PointBO> entityBOS = pointService.selectByDeviceId(deviceId);
            List<PointVO> entityVOS = pointBuilder.buildVOListByBOList(entityBOS);
            return R.ok(entityVOS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 分页查询 Point
     *
     * @param entityQuery Point Dto
     * @return Page Of Point
     */
    @PostMapping("/list")
    public R<Page<PointVO>> list(@RequestBody(required = false) PointQuery entityQuery) {
        try {
            if (ObjectUtil.isEmpty(entityQuery)) {
                entityQuery = new PointQuery();
            }
            entityQuery.setTenantId(getTenantId());
            Page<PointBO> entityPageBO = pointService.selectByPage(entityQuery);
            Page<PointVO> entityPageVO = pointBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
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
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
