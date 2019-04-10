/*
 * Copyright 2018 Google LLC. All Rights Reserved.
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
package com.pnoker.api.dbs.hystrix;

import com.pnoker.api.dbs.UserFeignApi;
import com.pnoker.common.util.wrapper.WrapMapper;
import com.pnoker.common.util.wrapper.Wrapper;
import org.springframework.stereotype.Component;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@Component
public class UserFeignApiHystrix implements UserFeignApi {
    @Override
    public Wrapper<String> getById(Long userId) {
        return WrapMapper.wrap(Wrapper.ERROR_CODE, Wrapper.ERROR_MESSAGE);
    }
}
