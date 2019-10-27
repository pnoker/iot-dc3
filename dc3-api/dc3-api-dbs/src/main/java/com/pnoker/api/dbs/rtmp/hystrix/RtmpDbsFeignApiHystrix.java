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

package com.pnoker.api.dbs.rtmp.hystrix;

import com.pnoker.api.dbs.rtmp.feign.RtmpDbsFeignApi;
import com.pnoker.common.dto.Dc3Page;
import com.pnoker.common.dto.transfer.RtmpDto;
import com.pnoker.common.model.rtmp.Rtmp;
import com.pnoker.common.utils.Response;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <p>
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@Component
public class RtmpDbsFeignApiHystrix implements FallbackFactory<RtmpDbsFeignApi> {

    @Override
    public RtmpDbsFeignApi create(Throwable throwable) {
        String message = throwable.getMessage() == null ? "No available server for client: DC3-DBS" : throwable.getMessage();
        log.error("RtmpFeignApi失败:{},hystrix服务降级处理", message, throwable);

        return new RtmpDbsFeignApi() {
            @Override
            public Response add(Rtmp rtmp) {
                return Response.fail(message);
            }

            @Override
            public Response delete(Long id) {
                return Response.fail(message);
            }

            @Override
            public Response<Boolean> update(Rtmp rtmp) {
                return Response.fail(message);
            }

            @Override
            public Response<Rtmp> selectById(Long id) {
                return Response.fail(message);
            }

            @Override
            public Response<Dc3Page<Rtmp>> list(RtmpDto rtmpDto) {
                return Response.fail(message);
            }
        };
    }
}