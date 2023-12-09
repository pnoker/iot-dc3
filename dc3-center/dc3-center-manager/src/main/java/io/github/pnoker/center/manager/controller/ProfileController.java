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
import io.github.pnoker.center.manager.entity.bo.ProfileBO;
import io.github.pnoker.center.manager.entity.query.ProfileBOPageQuery;
import io.github.pnoker.center.manager.service.ProfileService;
import io.github.pnoker.common.base.Controller;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.valid.Insert;
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
 * 模板 Controller
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerServiceConstant.PROFILE_URL_PREFIX)
public class ProfileController implements Controller {

    @Resource
    private ProfileService profileService;

    /**
     * 新增 Profile
     *
     * @param profileBO Profile
     * @return Profile
     */
    @PostMapping("/add")
    public R<String> add(@Validated(Insert.class) @RequestBody ProfileBO profileBO) {
        try {
            profileBO.setTenantId(getTenantId());
            profileService.save(profileBO);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 删除 Profile
     *
     * @param id 模板ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    public R<String> delete(@NotNull @PathVariable(value = "id") String id) {
        try {
            profileService.remove(Long.parseLong(id));
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 更新 Profile
     *
     * @param profileBO Profile
     * @return Profile
     */
    @PostMapping("/update")
    public R<String> update(@Validated(Update.class) @RequestBody ProfileBO profileBO) {
        try {
            profileBO.setTenantId(getTenantId());
            profileService.update(profileBO);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 Profile
     *
     * @param id 模板ID
     * @return Profile
     */
    @GetMapping("/id/{id}")
    public R<ProfileBO> selectById(@NotNull @PathVariable(value = "id") String id) {
        try {
            ProfileBO select = profileService.selectById(Long.parseLong(id));
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 根据 ID 集合查询 Profile
     *
     * @param profileIds Profile ID set
     * @return Map String:Profile
     */
    @PostMapping("/ids")
    public R<Map<Long, ProfileBO>> selectByIds(@RequestBody Set<String> profileIds) {
        try {
            Set<Long> collect = profileIds.stream().map(Long::parseLong).collect(Collectors.toSet());
            List<ProfileBO> profileBOS = profileService.selectByIds(collect);
            Map<Long, ProfileBO> profileMap = profileBOS.stream().collect(Collectors.toMap(ProfileBO::getId, Function.identity()));
            return R.ok(profileMap);
            // todo   返回 long id 前端你无法解析
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    /**
     * 根据 设备 ID 查询 Profile 集合
     *
     * @param deviceId 设备ID
     * @return Profile Array
     */
    @GetMapping("/device_id/{deviceId}")
    public R<List<ProfileBO>> selectByDeviceId(@NotNull @PathVariable(value = "deviceId") Long deviceId) {
        try {
            List<ProfileBO> select = profileService.selectByDeviceId(deviceId);
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 分页查询 Profile
     *
     * @param profilePageQuery Profile Dto
     * @return Page Of Profile
     */
    @PostMapping("/list")
    public R<Page<ProfileBO>> list(@RequestBody(required = false) ProfileBOPageQuery profilePageQuery) {
        try {
            if (ObjectUtil.isEmpty(profilePageQuery)) {
                profilePageQuery = new ProfileBOPageQuery();
            }
            profilePageQuery.setTenantId(getTenantId());
            Page<ProfileBO> page = profileService.selectByPage(profilePageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
