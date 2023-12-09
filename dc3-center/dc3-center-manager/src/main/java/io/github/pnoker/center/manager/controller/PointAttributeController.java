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
import io.github.pnoker.center.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.center.manager.entity.query.PointAttributeBOPageQuery;
import io.github.pnoker.center.manager.service.PointAttributeService;
import io.github.pnoker.common.base.Controller;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
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
@RequestMapping(ManagerServiceConstant.POINT_ATTRIBUTE_URL_PREFIX)
public class PointAttributeController implements Controller {

    @Resource
    private PointAttributeService pointAttributeService;

    /**
     * 新增 PointAttribute
     *
     * @param pointAttributeBO PointAttribute
     * @return PointAttribute
     */
    @PostMapping("/add")
    public R<PointAttributeBO> add(@Validated(Add.class) @RequestBody PointAttributeBO pointAttributeBO) {
        try {
            pointAttributeService.save(pointAttributeBO);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除 PointAttribute
     *
     * @param id 位号属性ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") String id) {
        try {
            pointAttributeService.remove(Long.parseLong(id));
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新 PointAttribute
     *
     * @param pointAttributeBO PointAttribute
     * @return PointAttribute
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody PointAttributeBO pointAttributeBO) {
        try {
            pointAttributeService.update(pointAttributeBO);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 PointAttribute
     *
     * @param id 位号属性ID
     * @return PointAttribute
     */
    @GetMapping("/id/{id}")
    public R<PointAttributeBO> selectById(@NotNull @PathVariable(value = "id") String id) {
        try {
            PointAttributeBO select = pointAttributeService.selectById(Long.parseLong(id));
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 根据 驱动ID 查询 PointAttribute
     *
     * @param id 位号属性ID
     * @return PointAttribute Array
     */
    @GetMapping("/driver_id/{id}")
    public R<List<PointAttributeBO>> selectByDriverId(@NotNull @PathVariable(value = "id") String id) {
        try {
            List<PointAttributeBO> select = pointAttributeService.selectByDriverId(Long.parseLong(id), true);
            if (CollUtil.isNotEmpty(select)) {
                return R.ok(select);
            }
        } catch (NotFoundException ne) {
            return R.ok(new ArrayList<>());
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 分页查询 PointAttribute
     *
     * @param pointAttributePageQuery 位号属性和分页参数
     * @return Page Of PointAttribute
     */
    @PostMapping("/list")
    public R<Page<PointAttributeBO>> list(@RequestBody(required = false) PointAttributeBOPageQuery pointAttributePageQuery) {
        try {
            if (ObjectUtil.isEmpty(pointAttributePageQuery)) {
                pointAttributePageQuery = new PointAttributeBOPageQuery();
            }
            Page<PointAttributeBO> page = pointAttributeService.selectByPage(pointAttributePageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
