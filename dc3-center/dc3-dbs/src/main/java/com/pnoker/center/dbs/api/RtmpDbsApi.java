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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * <p>RtmpDbsApi
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_DBS_RTMP_URL_PREFIX)
public class RtmpDbsApi implements RtmpDbsFeignClient {
    private final RtmpService rtmpService;

    public RtmpDbsApi(RtmpService rtmpService) {
        this.rtmpService = rtmpService;
    }

    @Override
    public Response<Rtmp> add(Rtmp rtmp) {
        try {
            rtmp = rtmpService.add(rtmp);
            return null != rtmp ? Response.ok(rtmp) : Response.fail("rtmp record add failed");
        } catch (Exception e) {
            return Response.fail(e.getMessage());
        }
    }

    @Override
    public Response<Boolean> delete(Long id) {
        try {
            return rtmpService.delete(id) ? Response.ok() : Response.fail("rtmp record delete failed");
        } catch (Exception e) {
            return Response.fail(e.getMessage());
        }
    }

    @Override
    public Response<Rtmp> update(Rtmp rtmp) {
        if (null == rtmp.getId()) {
            return Response.fail("id is null");
        }
        try {
            rtmp = rtmpService.update(rtmp);
            return null != rtmp ? Response.ok(rtmp) : Response.fail("rtmp record update failed");
        } catch (Exception e) {
            return Response.fail(e.getMessage());
        }
    }

    @Override
    public Response<Rtmp> selectById(Long id) {
        try {
            Rtmp rtmp = rtmpService.selectById(id);
            return null != rtmp ? Response.ok(rtmp) : Response.fail(String.format("rtmp record does not exist for id(%s)", id));
        } catch (Exception e) {
            return Response.fail(e.getMessage());
        }
    }

    @Override
    public Response<Page<Rtmp>> list(RtmpDto rtmpDto) {
        if (!Optional.ofNullable(rtmpDto).isPresent()) {
            rtmpDto = new RtmpDto();
        }
        try {
            return Response.ok(rtmpService.list(rtmpDto));
        } catch (Exception e) {
            return Response.fail(e.getMessage());
        }
    }

}
