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

package com.pnoker.api.center.device.hystrix;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.center.device.feign.ProfileInfoClient;
import com.pnoker.common.bean.R;
import com.pnoker.common.dto.ProfileInfoDto;
import com.pnoker.common.model.Dic;
import com.pnoker.common.model.ProfileInfo;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>驱动属性配置信息 FeignHystrix
 *
 * @author pnoker
 */
@Slf4j
@Component
public class ProfileInfoClientHystrix implements FallbackFactory<ProfileInfoClient> {

    @Override
    public ProfileInfoClient create(Throwable throwable) {
        String message = throwable.getMessage() == null ? "No available server for client: DC3-MANAGER" : throwable.getMessage();
        log.error("ProfileInfoClientHystrix:{},hystrix服务降级处理", message, throwable);

        return new ProfileInfoClient() {

            @Override
            public R<ProfileInfo> add(ProfileInfo profileInfo) {
                return R.fail(message);
            }

            @Override
            public R<Boolean> delete(Long id) {
                return R.fail(message);
            }

            @Override
            public R<ProfileInfo> update(ProfileInfo profileInfo) {
                return R.fail(message);
            }

            @Override
            public R<ProfileInfo> selectById(Long id) {
                return R.fail(message);
            }

            @Override
            public R<ProfileInfo> selectByName(String name) {
                return R.fail(message);
            }

            @Override
            public R<Page<ProfileInfo>> list(ProfileInfoDto profileInfoDto) {
                return R.fail(message);
            }

            @Override
            public R<List<Dic>> dictionary() {
                return R.fail(message);
            }
        };
    }
}