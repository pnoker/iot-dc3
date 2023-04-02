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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.PointAttributePageQuery;
import io.github.pnoker.center.manager.service.PointAttributeService;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.model.PointAttribute;
import io.github.pnoker.common.valid.Insert;
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
public class PointAttributeController {

    @Resource
    private PointAttributeService pointAttributeService;

    /**
     * 新增 PointAttribute
     *
     * @param pointAttribute PointAttribute
     * @return PointAttribute
     */
    @PostMapping("/add")
    public R<PointAttribute> add(@Validated(Insert.class) @RequestBody PointAttribute pointAttribute) {
        try {
            pointAttributeService.add(pointAttribute);
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
            pointAttributeService.delete(id);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 修改 PointAttribute
     *
     * @param pointAttribute PointAttribute
     * @return PointAttribute
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody PointAttribute pointAttribute) {
        try {
            pointAttributeService.update(pointAttribute);
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
    public R<PointAttribute> selectById(@NotNull @PathVariable(value = "id") String id) {
        try {
            PointAttribute select = pointAttributeService.selectById(id);
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
    public R<List<PointAttribute>> selectByDriverId(@NotNull @PathVariable(value = "id") String id) {
        try {
            List<PointAttribute> select = pointAttributeService.selectByDriverId(id, true);
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
     * 模糊分页查询 PointAttribute
     *
     * @param pointAttributePageQuery 位号属性和分页参数
     * @return Page Of PointAttribute
     */
    @PostMapping("/list")
    public R<Page<PointAttribute>> list(@RequestBody(required = false) PointAttributePageQuery pointAttributePageQuery) {
        try {
            if (ObjectUtil.isEmpty(pointAttributePageQuery)) {
                pointAttributePageQuery = new PointAttributePageQuery();
            }
            Page<PointAttribute> page = pointAttributeService.list(pointAttributePageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
