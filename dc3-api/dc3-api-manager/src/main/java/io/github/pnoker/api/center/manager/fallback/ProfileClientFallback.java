/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.api.center.manager.fallback;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.feign.ProfileClient;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.api.center.manager.dto.ProfileDto;
import io.github.pnoker.common.model.Profile;
import io.github.pnoker.common.utils.ExceptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ProfileClientFallback
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Component
public class ProfileClientFallback implements FallbackFactory<ProfileClient> {

    @Override
    public ProfileClient create(Throwable throwable) {
        String message = ExceptionUtil.getNotAvailableServiceMessage(ManagerServiceConstant.SERVICE_NAME, throwable.getMessage());
        log.error("Fallback:{}", message);

        return new ProfileClient() {

            /**
             * {@inheritDoc}
             */
            @Override
            public R<Profile> add(Profile profile, String tenantId) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<Boolean> delete(String id) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<Profile> update(Profile profile, String tenantId) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<Profile> selectById(String id) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<Map<String, Profile>> selectByIds(Set<String> profileIds) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<List<Profile>> selectByDeviceId(String deviceId) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<Page<Profile>> list(ProfileDto profileDto, String tenantId) {
                return R.fail(message);
            }

        };
    }
}