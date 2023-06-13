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
import io.github.pnoker.center.manager.entity.query.LabelPageQuery;
import io.github.pnoker.center.manager.service.LabelService;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.model.Label;
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

/**
 * 标签 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerServiceConstant.LABEL_URL_PREFIX)
public class LabelController {

    @Resource
    private LabelService labelService;

    /**
     * 新增 Label
     *
     * @param label    Label
     * @param tenantId 租户ID
     * @return Label
     */
    @PostMapping("/add")
    public R<String> add(@Validated(Insert.class) @RequestBody Label label,
                         @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            label.setTenantId(tenantId);
            labelService.add(label);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除 Label
     *
     * @param id 标签ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") String id) {
        try {
            labelService.delete(id);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 修改 Label
     *
     * @param label    Label
     * @param tenantId 租户ID
     * @return Label
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody Label label,
                            @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            label.setTenantId(tenantId);
            labelService.update(label);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 Label
     *
     * @param id 标签ID
     * @return Label
     */
    @GetMapping("/id/{id}")
    public R<Label> selectById(@NotNull @PathVariable(value = "id") String id) {
        try {
            Label select = labelService.selectById(id);
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 模糊分页查询 Label
     *
     * @param labelPageQuery Label Dto
     * @param tenantId       租户ID
     * @return Page Of Label
     */
    @PostMapping("/list")
    public R<Page<Label>> list(@RequestBody(required = false) LabelPageQuery labelPageQuery,
                               @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            if (ObjectUtil.isEmpty(labelPageQuery)) {
                labelPageQuery = new LabelPageQuery();
            }
            labelPageQuery.setTenantId(tenantId);
            Page<Label> page = labelService.list(labelPageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
}
