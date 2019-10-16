/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.pnoker.transfer.opc.controller;

import com.pnoker.common.base.BaseController;
import com.pnoker.common.model.dto.Response;
import com.pnoker.transfer.opc.bean.OpcInfo;
import com.pnoker.transfer.opc.bean.OpcServer;
import com.pnoker.transfer.opc.service.OpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@RestController
public class OpcController extends BaseController {
    @Autowired
    private OpcService opcService;

    @RequestMapping(value = "/list/{type}", method = RequestMethod.GET)
    public Response list(@PathVariable String type, OpcInfo info) {
        switch (type) {
            case "server":
                try {
                    return Response.ok(opcService.opcServerList(info));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    return Response.fail(e.getMessage());
                }
            case "item":
                try {
                    List<OpcServer> opcServerList = opcService.opcServerList(info);
                    for (OpcServer opcServer : opcServerList) {
                        if (opcServer.getClsId().equals(info.getClsId())) {
                            info.setProgId(opcServer.getProgId());
                            return Response.ok(opcService.opcItemList(info));
                        }
                    }
                    return Response.fail(String.format("No such Opc Server with this clsid (%s)", info.getClsId()));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    return Response.fail(e.getMessage());
                }
            default:
                return Response.fail("This type is not supported");
        }
    }

    @RequestMapping(value = "/value/{type}", method = RequestMethod.GET)
    public Response value(@PathVariable String type, List<String> nodes) {
        List<String> listA = new ArrayList<>();
        List<String> listB = new ArrayList<>();
        listA.retainAll(listB);
        switch (type) {
            case "node":
                return Response.ok();
            case "nodes":
                return Response.ok();
            default:
                return Response.fail("This node is not existent");
        }
    }
}
