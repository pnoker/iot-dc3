/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pnoker.device.manager.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.device.manager.feign.ProfileClient;
import com.pnoker.common.bean.R;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.ProfileDto;
import com.pnoker.common.model.Driver;
import com.pnoker.common.model.Profile;
import com.pnoker.device.manager.service.ProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>模板 Client 接口实现
 *
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_MANAGER_PROFILE_URL_PREFIX)
public class ProfileApi implements ProfileClient {

    @Resource
    private ProfileService profileService;

    @Override
    public R<Profile> add(Profile profile) {
        try {
            Profile add = profileService.add(profile);
            if (null != add) {
                return R.ok(add);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Boolean> delete(Long id) {
        try {
            return profileService.delete(id) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<Profile> update(Profile profile) {
        try {
            Profile update = profileService.update(profile);
            if (null != update) {
                return R.ok(update);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Profile> selectById(Long id) {
        try {
            Profile select = profileService.selectById(id);
            if (null != select) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Profile> selectByName(String name) {
        try {
            Profile select = profileService.selectByName(name);
            if (null != select) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Page<Profile>> list(ProfileDto profileDto) {
        try {
            Page<Profile> page = profileService.list(profileDto);
            if (null != page) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Profile>> dictionary() {
        try {
            List<Profile> list = profileService.dictionary();
            if (null != list) {
                return R.ok(list);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }
}
