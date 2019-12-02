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

package com.pnoker.transfer.rtmp.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.transfer.rtmp.feign.RtmpTransferFeignApi;
import com.pnoker.common.base.bean.Response;
import com.pnoker.common.base.dto.PageInfo;
import com.pnoker.common.base.dto.transfer.RtmpDto;
import com.pnoker.common.base.model.rtmp.Rtmp;
import com.pnoker.transfer.rtmp.service.RtmpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * <p>Rest接口控制器
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@RestController
public class RtmpTransferRestClient implements RtmpTransferFeignApi {
    @Autowired
    private RtmpService rtmpService;

    @Override
    public Response<Long> add(@RequestBody Rtmp rtmp) {
        if (!Optional.ofNullable(rtmp).isPresent()) {
            return Response.fail("body is null");
        }
        return rtmpService.add(rtmp).isOk() ? Response.ok(rtmp.getId()) : Response.fail();
    }

    @Override
    public Response<Boolean> delete(@PathVariable Long id) {
        if (null == id) {
            return Response.fail("id can not be empty");
        }
        return rtmpService.delete(id);
    }

    @Override
    public Response<Boolean> update(@RequestBody Rtmp rtmp) {
        if (!Optional.ofNullable(rtmp).isPresent()) {
            return Response.fail("body is null");
        }
        return rtmpService.update(rtmp);
    }

    @Override
    public Response<Rtmp> selectById(@PathVariable Long id) {
        if (null == id) {
            return Response.fail("id can not be empty");
        }
        return rtmpService.selectById(id);
    }

    @Override
    public Response<Page<Rtmp>> list(@RequestBody(required = false) RtmpDto rtmpDto) {
        Rtmp rtmp = new Rtmp();
        PageInfo page = new PageInfo();
        Optional.ofNullable(rtmpDto).ifPresent(r -> {
            BeanUtils.copyProperties(r, rtmp);
            Optional.ofNullable(rtmpDto.getPage()).ifPresent(p -> BeanUtils.copyProperties(p, page));
        });
        return rtmpService.list(rtmp, page);
    }

    @Override
    public Response<Boolean> start(@PathVariable Long id) {
        if (null == id) {
            return Response.fail("id can not be empty");
        }
        return rtmpService.start(id);
    }

    @Override
    public Response<Boolean> stop(@PathVariable Long id) {
        if (null == id) {
            return Response.fail("id can not be empty");
        }
        return rtmpService.stop(id);
    }

}
