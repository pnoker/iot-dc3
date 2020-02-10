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

package com.pnoker.api.center.device.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.center.device.hystrix.ProfileClientHystrix;
import com.pnoker.common.bean.R;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.ProfileDto;
import com.pnoker.common.model.Dic;
import com.pnoker.common.model.Profile;
import com.pnoker.common.valid.Insert;
import com.pnoker.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * <p>位号 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = Common.Service.DC3_DEVICE_PROFILE_URL_PREFIX, name = Common.Service.DC3_DEVICE, fallbackFactory = ProfileClientHystrix.class)
public interface ProfileClient {

    /**
     * 新增 Profile 记录
     *
     * @param profile
     * @return Profile
     */
    @PostMapping("/add")
    R<Profile> add(@Validated(Insert.class) @RequestBody Profile profile);

    /**
     * 根据 ID 删除 Profile
     *
     * @param id profileId
     * @return Boolean
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@PathVariable(value = "id") Long id);

    /**
     * 修改 Profile 记录
     *
     * @param profile
     * @return Profile
     */
    @PostMapping("/update")
    R<Profile> update(@Validated(Update.class) @RequestBody Profile profile);

    /**
     * 根据 ID 查询 Profile
     *
     * @param id
     * @return Profile
     */
    @GetMapping("/id/{id}")
    R<Profile> selectById(@PathVariable(value = "id") Long id);

    /**
     * 根据 Name 查询 Profile
     *
     * @param name
     * @return Profile
     */
    @GetMapping("/name/{name}")
    R<Profile> selectByName(@PathVariable(value = "name") String name);

    /**
     * 分页查询 Profile
     *
     * @param profileDto
     * @return Page<Profile>
     */
    @PostMapping("/list")
    R<Page<Profile>> list(@RequestBody(required = false) ProfileDto profileDto);

    /**
     * 查询 Profile 字典
     *
     * @return List<Profile>
     */
    @GetMapping("/dictionary")
    R<List<Dic>> dictionary();

}
