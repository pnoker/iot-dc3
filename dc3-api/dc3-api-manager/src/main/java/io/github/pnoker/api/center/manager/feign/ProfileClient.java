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

package io.github.pnoker.api.center.manager.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.fallback.ProfileClientFallback;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.ServiceConstant;
import io.github.pnoker.common.dto.ProfileDto;
import io.github.pnoker.common.model.Profile;
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 模板 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = ServiceConstant.Manager.PROFILE_URL_PREFIX, name = ServiceConstant.Manager.SERVICE_NAME, fallbackFactory = ProfileClientFallback.class)
public interface ProfileClient {

    /**
     * 新增 Profile
     *
     * @param profile Profile
     * @return Profile
     */
    @PostMapping("/add")
    R<Profile> add(@Validated(Insert.class) @RequestBody Profile profile, @RequestHeader(value = ServiceConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 根据 ID 删除 Profile
     *
     * @param id profile Id
     * @return Boolean
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@NotNull @PathVariable(value = "id") String id);

    /**
     * 修改 Profile
     *
     * @param profile Profile
     * @return Profile
     */
    @PostMapping("/update")
    R<Profile> update(@Validated(Update.class) @RequestBody Profile profile, @RequestHeader(value = ServiceConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 根据 ID 查询 Profile
     *
     * @param id Profile Id
     * @return Profile
     */
    @GetMapping("/id/{id}")
    R<Profile> selectById(@NotNull @PathVariable(value = "id") String id);

    /**
     * 根据 ID 集合查询 Profile
     *
     * @param profileIds Profile Id set
     * @return Map<String, Profile>
     */
    @PostMapping("/ids")
    R<Map<String, Profile>> selectByIds(@RequestBody Set<String> profileIds);

    /**
     * 根据 设备 ID 查询 Profile 集合
     *
     * @param deviceId Device Id
     * @return Profile Array
     */
    @GetMapping("/device_id/{deviceId}")
    R<List<Profile>> selectByDeviceId(@NotNull @PathVariable(value = "deviceId") String deviceId);

    /**
     * 分页查询 Profile
     *
     * @param profileDto Profile Dto
     * @return Page<Profile>
     */
    @PostMapping("/list")
    R<Page<Profile>> list(@RequestBody(required = false) ProfileDto profileDto, @RequestHeader(value = ServiceConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

}
