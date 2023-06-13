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
import io.github.pnoker.center.manager.entity.query.GroupPageQuery;
import io.github.pnoker.center.manager.service.GroupService;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.model.Group;
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

/**
 * 设备 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerServiceConstant.GROUP_URL_PREFIX)
public class GroupController {

    @Resource
    private GroupService groupService;

    /**
     * 新增 Group
     *
     * @param group    Group
     * @param tenantId 租户ID
     * @return Group
     */
    @PostMapping("/add")
    public R<String> add(@Validated(Insert.class) @RequestBody Group group,
                         @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            group.setTenantId(tenantId);
            groupService.add(group);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除 Group
     *
     * @param id 分组ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") String id) {
        try {
            groupService.delete(id);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 修改 Group
     *
     * @param group    Group
     * @param tenantId 租户ID
     * @return Group
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody Group group,
                            @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            group.setTenantId(tenantId);
            groupService.update(group);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 Group
     *
     * @param id 分组ID
     * @return Group
     */
    @GetMapping("/id/{id}")
    public R<Group> selectById(@NotNull @PathVariable(value = "id") String id) {
        try {
            Group select = groupService.selectById(id);
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 模糊分页查询 Group
     *
     * @param groupPageQuery Group Dto
     * @param tenantId       租户ID
     * @return Page Of Group
     */
    @PostMapping("/list")
    public R<Page<Group>> list(@RequestBody(required = false) GroupPageQuery groupPageQuery,
                               @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = DefaultConstant.DEFAULT_ID) String tenantId) {
        try {
            if (ObjectUtil.isEmpty(groupPageQuery)) {
                groupPageQuery = new GroupPageQuery();
            }
            groupPageQuery.setTenantId(tenantId);
            Page<Group> page = groupService.list(groupPageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
