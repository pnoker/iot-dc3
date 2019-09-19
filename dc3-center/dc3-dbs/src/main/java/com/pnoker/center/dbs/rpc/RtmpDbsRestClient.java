/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.pnoker.center.dbs.rpc;

import com.pnoker.center.dbs.service.RtmpService;
import com.pnoker.common.base.BaseController;
import com.pnoker.common.model.domain.rtmp.Rtmp;
import com.pnoker.common.model.dto.Response;
import com.pnoker.transfer.rtmp.feign.RtmpDbsFeignApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class RtmpDbsRestClient extends BaseController implements RtmpDbsFeignApi {
    @Autowired
    private RtmpService rtmpService;

    @Override
    public Response<Boolean> add(@RequestBody Rtmp rtmp) {
        return null;
    }

    @Override
    public Response<Boolean> delete(@RequestParam(value = "id") String id) {
        return Response.ok();
    }

    @Override
    public Response<Boolean> update(@RequestBody Rtmp rtmp) {
        return null;
    }

    @Override
    public Response<List<Rtmp>> list() {
        List<Rtmp> list = rtmpService.list();
        return Response.ok(list);
    }
}
