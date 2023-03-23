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
import io.github.pnoker.center.manager.entity.query.ProfilePageQuery;
import io.github.pnoker.center.manager.service.NotifyService;
import io.github.pnoker.center.manager.service.ProfileService;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.model.Profile;
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
public class ProfileController {

    @Resource
    private ProfileService profileService;

    @Resource
    private NotifyService notifyService;

    /**
     * 新增 Profile
     *
     * @param profile  Profile
     * @param tenantId 租户ID
     * @return Profile
     */
    @PostMapping("/add")
    public R<Profile> add(@Validated(Insert.class) @RequestBody Profile profile,
                          @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId) {
        try {
            profile.setTenantId(tenantId);
            Profile add = profileService.add(profile);
            if (ObjectUtil.isNotNull(add)) {
                return R.ok(add);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 根据 ID 删除 Profile
     *
     * @param id 模板ID
     * @return 是否删除
     */
    @PostMapping("/delete/{id}")
    public R<Boolean> delete(@NotNull @PathVariable(value = "id") String id) {
        try {
            Profile profile = profileService.selectById(id);
            if (ObjectUtil.isNotNull(profile) && profileService.delete(id)) {
                notifyService.notifyDriverProfile(MetadataCommandTypeEnum.DELETE, profile);
                return R.ok();
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 修改 Profile
     *
     * @param profile  Profile
     * @param tenantId 租户ID
     * @return Profile
     */
    @PostMapping("/update")
    public R<Profile> update(@Validated(Update.class) @RequestBody Profile profile,
                             @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId) {
        try {
            profile.setTenantId(tenantId);
            Profile update = profileService.update(profile);
            if (ObjectUtil.isNotNull(update)) {
                notifyService.notifyDriverProfile(MetadataCommandTypeEnum.UPDATE, update);
                return R.ok(update);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 根据 ID 查询 Profile
     *
     * @param id 模板ID
     * @return Profile
     */
    @GetMapping("/id/{id}")
    public R<Profile> selectById(@NotNull @PathVariable(value = "id") String id) {
        try {
            Profile select = profileService.selectById(id);
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
    public R<Map<String, Profile>> selectByIds(@RequestBody Set<String> profileIds) {
        try {
            List<Profile> profiles = profileService.selectByIds(profileIds);
            Map<String, Profile> profileMap = profiles.stream().collect(Collectors.toMap(Profile::getId, Function.identity()));
            return R.ok(profileMap);
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
    public R<List<Profile>> selectByDeviceId(@NotNull @PathVariable(value = "deviceId") String deviceId) {
        try {
            List<Profile> select = profileService.selectByDeviceId(deviceId);
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    /**
     * 模糊分页查询 Profile
     *
     * @param profilePageQuery Profile Dto
     * @param tenantId         租户ID
     * @return Page Of Profile
     */
    @PostMapping("/list")
    public R<Page<Profile>> list(@RequestBody(required = false) ProfilePageQuery profilePageQuery,
                                 @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId) {
        try {
            if (ObjectUtil.isEmpty(profilePageQuery)) {
                profilePageQuery = new ProfilePageQuery();
            }
            profilePageQuery.setTenantId(tenantId);
            Page<Profile> page = profileService.list(profilePageQuery);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
