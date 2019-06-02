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

import com.pnoker.api.dbs.feign.RtmpFeignApi;
import com.pnoker.common.model.rtmp.Rtmp;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@Slf4j
@Component
public class RtmpFeignApiHystrix implements FallbackFactory<RtmpFeignApi> {

    @Override
    public RtmpFeignApi create(Throwable throwable) {
        log.error("{}", throwable.getMessage(), throwable);
        return new RtmpFeignApi() {
            @Override
            public String api() {
                return "api() 故障，返回默认值：0";
            }

            @Override
            public String add(String json) {
                return null;
            }

            @Override
            public String delete(String json) {
                return null;
            }

            @Override
            public String update(String json) {
                return null;
            }

            @Override
            public List<Rtmp> list() {
                log.info("报错");
                return null;
            }
        };
    }
}
