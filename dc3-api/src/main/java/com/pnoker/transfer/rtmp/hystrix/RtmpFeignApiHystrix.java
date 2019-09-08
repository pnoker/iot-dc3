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

package com.pnoker.transfer.rtmp.hystrix;

import com.pnoker.common.bean.base.Response;
import com.pnoker.transfer.rtmp.feign.RtmpFeignApi;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>@Author    : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@Slf4j
@Component
public class RtmpFeignApiHystrix implements FallbackFactory<RtmpFeignApi> {

    @Override
    public RtmpFeignApi create(Throwable throwable) {
        String message = throwable.getMessage() == null ? "No available server for client: DC3-DBS" : throwable.getMessage();
        log.error("RtmpFeignApi,进入熔断:{}", message, throwable);

        return new RtmpFeignApi() {

            @Override
            public Response add(String json) {
                return fail(throwable);
            }

            @Override
            public Response delete(String json) {
                return fail(throwable);
            }

            @Override
            public Response update(String json) {
                return fail(throwable);
            }

            @Override
            public Response list(String json) {
                return fail(throwable);
            }
        };
    }

    public Response fail(Throwable throwable) {
        return new Response().fail(throwable.getMessage());
    }
}
