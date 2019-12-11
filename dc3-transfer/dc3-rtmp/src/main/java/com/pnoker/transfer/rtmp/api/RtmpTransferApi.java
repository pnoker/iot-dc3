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

package com.pnoker.transfer.rtmp.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.transfer.rtmp.feign.RtmpTransferFeignClient;
import com.pnoker.common.base.bean.Response;
import com.pnoker.common.base.constant.Common;
import com.pnoker.common.base.bean.PageInfo;
import com.pnoker.common.base.dto.transfer.RtmpDto;
import com.pnoker.common.base.entity.rtmp.Rtmp;
import com.pnoker.transfer.rtmp.service.RtmpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * <p>Rest接口控制器
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_RTMP_URL_PREFIX)
public class RtmpTransferApi implements RtmpTransferFeignClient {
    @Resource
    private RtmpService rtmpService;

    @Override
    public Response<Long> add(Rtmp rtmp) {
        if (!Optional.ofNullable(rtmp).isPresent()) {
            return Response.fail("body is null");
        }
        return rtmpService.add(rtmp).isOk() ? Response.ok(rtmp.getId()) : Response.fail();
    }

    @Override
    public Response<Boolean> delete(Long id) {
        return rtmpService.delete(id);
    }

    @Override
    public Response<Boolean> update(Rtmp rtmp) {
        if (!Optional.ofNullable(rtmp).isPresent()) {
            return Response.fail("body is null");
        }
        return rtmpService.update(rtmp);
    }

    @Override
    public Response<Rtmp> selectById(Long id) {
        return rtmpService.selectById(id);
    }

    @Override
    public Response<Page<Rtmp>> list(RtmpDto rtmpDto) {
        Rtmp rtmp = new Rtmp();
        PageInfo page = new PageInfo();
        Optional.ofNullable(rtmpDto).ifPresent(r -> {
            BeanUtils.copyProperties(r, rtmp);
            Optional.ofNullable(rtmpDto.getPage()).ifPresent(p -> BeanUtils.copyProperties(p, page));
        });
        return rtmpService.list(rtmp, page);
    }

    @Override
    public Response<Boolean> start(Long id) {
        return rtmpService.start(id);
    }

    @Override
    public Response<Boolean> stop(Long id) {
        return rtmpService.stop(id);
    }

}
