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
import io.github.pnoker.center.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.center.manager.entity.builder.PointAttributeBuilder;
import io.github.pnoker.center.manager.entity.query.PointAttributeQuery;
import io.github.pnoker.center.manager.entity.vo.PointAttributeVO;
import io.github.pnoker.center.manager.service.PointAttributeService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * 驱动属性配置信息 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@Tag(name = "接口-位号属性")
@RequestMapping(ManagerConstant.POINT_ATTRIBUTE_URL_PREFIX)
public class PointAttributeController implements BaseController {

    @Resource
    private PointAttributeBuilder pointAttributeBuilder;

    @Resource
    private PointAttributeService pointAttributeService;

    /**
     * 新增 PointAttribute
     *
     * @param entityVO {@link PointAttributeVO}
     * @return R of String
     */
    @PostMapping("/add")
    @Operation(summary = "新增-位号属性")
    public R<PointAttributeBO> add(@Validated(Add.class) @RequestBody PointAttributeVO entityVO) {
        try {
            PointAttributeBO entityBO = pointAttributeBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(getTenantId());
            pointAttributeService.save(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除 PointAttribute
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            pointAttributeService.remove(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新 PointAttribute
     *
     * @param entityVO {@link PointAttributeVO}
     * @return R of String
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody PointAttributeVO entityVO) {
        try {
            PointAttributeBO entityBO = pointAttributeBuilder.buildBOByVO(entityVO);
            pointAttributeService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 PointAttribute
     *
     * @param id ID
     * @return PointAttributeVO {@link PointAttributeVO}
     */
    @GetMapping("/id/{id}")
    public R<PointAttributeVO> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            PointAttributeBO entityBO = pointAttributeService.selectById(id);
            PointAttributeVO entityVO = pointAttributeBuilder.buildVOByBO(entityBO);
            return R.ok(entityVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 驱动ID 查询 PointAttribute
     *
     * @param id 位号属性ID
     * @return PointAttribute Array
     */
    @GetMapping("/driver_id/{id}")
    public R<List<PointAttributeVO>> selectByDriverId(@NotNull @PathVariable(value = "id") Long id) {
        try {
            List<PointAttributeBO> entityBOS = pointAttributeService.selectByDriverId(id, true);
            List<PointAttributeVO> entityVO = pointAttributeBuilder.buildVOListByBOList(entityBOS);
            return R.ok(entityVO);
        } catch (NotFoundException ne) {
            return R.ok(new ArrayList<>());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 分页查询 PointAttribute
     *
     * @param entityQuery 位号属性和分页参数
     * @return Page Of PointAttribute
     */
    @PostMapping("/list")
    public R<Page<PointAttributeVO>> list(@RequestBody(required = false) PointAttributeQuery entityQuery) {
        try {
            if (ObjectUtil.isEmpty(entityQuery)) {
                entityQuery = new PointAttributeQuery();
            }
            entityQuery.setTenantId(getTenantId());
            Page<PointAttributeBO> entityPageBO = pointAttributeService.selectByPage(entityQuery);
            Page<PointAttributeVO> entityPageVO = pointAttributeBuilder.buildVOPageByBOPage(entityPageBO);
            return R.ok(entityPageVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(e.getMessage());
        }
    }

}
