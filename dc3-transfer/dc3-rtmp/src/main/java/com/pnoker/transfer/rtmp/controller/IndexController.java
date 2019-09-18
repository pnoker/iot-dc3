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

package com.pnoker.transfer.rtmp.controller;

import com.pnoker.common.base.BaseController;
import com.pnoker.common.model.domain.rtmp.Rtmp;
import com.pnoker.common.model.dto.Response;
import com.pnoker.transfer.rtmp.model.constant.Global;
import com.pnoker.transfer.rtmp.model.dto.CmdTask;
import com.pnoker.transfer.rtmp.model.vo.RtmpVo;
import com.pnoker.transfer.rtmp.service.RtmpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>@Author    : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Rest接口控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v3/rtmp")
public class IndexController extends BaseController {
    @Autowired
    private RtmpService rtmpService;

    @PostMapping("/add")
    public Response add(@RequestBody RtmpVo rtmpVo) {
        Rtmp rtmp = new Rtmp();
        BeanUtils.copyProperties(rtmpVo, rtmp);
        boolean result = rtmpService.createCmdTask(rtmp);

        if (result) {
            return Response.ok();
        } else {
            return Response.fail();
        }
    }

    @DeleteMapping("/delete")
    public Response delete(String id) {
        CmdTask cmdTask = Global.taskMap.get(id);
        boolean result = cmdTask.stop();
        cmdTask.clear();
        Global.taskMap.remove(id);

        if (result) {
            return Response.ok();
        } else {
            return Response.fail();
        }
    }

    @GetMapping("/list")
    public Response<List<CmdTask>> list() {
        List<CmdTask> list;
        Collection<CmdTask> collection = Global.taskMap.values();
        if (collection instanceof List) {
            list = (List) collection;
        } else {
            list = new ArrayList(collection);
        }
        return Response.ok(list);
    }

    @PostMapping("/stop")
    public Response stop(String id) {
        CmdTask cmdTask = Global.taskMap.get(id);
        boolean result = cmdTask.stop();

        if (result) {
            return Response.ok();
        } else {
            return Response.fail();
        }
    }

}
