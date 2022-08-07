/*
 * Copyright 2022 Pnoker All Rights Reserved
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

package io.github.pnoker.center.manager.api;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.feign.ProfileClient;
import io.github.pnoker.center.manager.service.NotifyService;
import io.github.pnoker.center.manager.service.ProfileService;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.CommonConstant;
import io.github.pnoker.common.constant.ServiceConstant;
import io.github.pnoker.common.dto.PointInfoDto;
import io.github.pnoker.common.dto.ProfileDto;
import io.github.pnoker.common.model.Profile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 模板 Client 接口实现
 *
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(ServiceConstant.Manager.PROFILE_URL_PREFIX)
public class ProfileApi implements ProfileClient {

    @Resource
    private ProfileService profileService;

    @Resource
    private NotifyService notifyService;

    @Override
    public R<Profile> add(Profile profile, String tenantId) {
        try {
            Profile add = profileService.add(profile.setTenantId(tenantId));
            if (ObjectUtil.isNotNull(add)) {
                return R.ok(add);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Boolean> delete(String id) {
        try {
            Profile profile = profileService.selectById(id);
            if (ObjectUtil.isNotNull(profile) && profileService.delete(id)) {
                notifyService.notifyDriverProfile(CommonConstant.Driver.Profile.DELETE, profile);
                return R.ok();
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Profile> update(Profile profile, String tenantId) {
        try {
            Profile update = profileService.update(profile.setTenantId(tenantId));
            if (ObjectUtil.isNotNull(update)) {
                notifyService.notifyDriverProfile(CommonConstant.Driver.Profile.UPDATE, update);
                return R.ok(update);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Profile> selectById(String id) {
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

    @Override
    public R<Map<String, Profile>> selectByIds(Set<String> profileIds) {
        try {
            List<Profile> profiles = profileService.selectByIds(profileIds);
            Map<String, Profile> profileMap = profiles.stream().collect(Collectors.toMap(Profile::getId, Function.identity()));
            return R.ok(profileMap);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<List<Profile>> selectByDeviceId(String deviceId) {
        try {
            List<Profile> select = profileService.selectByDeviceId(deviceId);
            if (null != select) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Page<Profile>> list(ProfileDto profileDto, String tenantId) {
        try {
            if (ObjectUtil.isEmpty(profileDto)) {
                profileDto = new ProfileDto();
            }
            profileDto.setTenantId(tenantId);
            Page<Profile> page = profileService.list(profileDto);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
