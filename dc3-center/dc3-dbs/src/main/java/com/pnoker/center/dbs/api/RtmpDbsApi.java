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

package com.pnoker.center.dbs.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.center.dbs.rtmp.feign.RtmpDbsFeignClient;
import com.pnoker.center.dbs.service.RtmpService;
import com.pnoker.common.bean.Response;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.transfer.RtmpDto;
import com.pnoker.common.entity.rtmp.Rtmp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * <p>rtmp dbs rest api
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_DBS_RTMP_URL_PREFIX)
public class RtmpDbsApi implements RtmpDbsFeignClient {
    @Autowired
    private RtmpService rtmpService;

    @Override
    public Response<Long> add(Rtmp rtmp) {
        return null != rtmpService.add(rtmp) ? Response.ok(rtmp.getId()) : Response.fail();
    }

    @Override
    public Response<Boolean> delete(Long id) {
        return rtmpService.delete(id) ? Response.ok() : Response.fail();
    }

    @Override
    public Response<Boolean> update(Rtmp rtmp) {
        if (null == rtmp.getId()) {
            return Response.fail("id is null");
        }
        return null != rtmpService.update(rtmp) ? Response.ok() : Response.fail();
    }

    @Override
    public Response<Rtmp> selectById(Long id) {
        Rtmp rtmp = rtmpService.selectById(id);
        return null != rtmp ? Response.ok(rtmp) : Response.fail("id does not exist");
    }

    @Override
    public Response<Page<Rtmp>> list(RtmpDto rtmpDto) {
        if (!Optional.ofNullable(rtmpDto).isPresent()) {
            rtmpDto = new RtmpDto();
        }
        return Response.ok(rtmpService.list(rtmpDto));
    }

}
