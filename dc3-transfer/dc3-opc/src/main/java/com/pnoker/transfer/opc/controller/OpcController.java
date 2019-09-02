package com.pnoker.transfer.opc.controller;

import com.pnoker.common.base.BaseController;
import com.pnoker.common.bean.base.Response;
import com.pnoker.transfer.opc.bean.OpcInfo;
import com.pnoker.transfer.opc.bean.OpcServer;
import com.pnoker.transfer.opc.service.OpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Opc
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
                    return ok(opcService.opcServerList(info));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    return fail(e.getMessage());
                }
            case "item":
                try {
                    List<OpcServer> opcServerList = opcService.opcServerList(info);
                    for (OpcServer opcServer : opcServerList) {
                        if (opcServer.getClsId().equals(info.getClsId())) {
                            info.setProgId(opcServer.getProgId());
                            return ok(opcService.opcItemList(info));
                        }
                    }
                    return fail(String.format("No such Opc Server with this clsid (%s)", info.getClsId()));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    return fail(e.getMessage());
                }
            default:
                return fail("This type is not supported");
        }
    }

    @RequestMapping(value = "/value/{type}", method = RequestMethod.GET)
    public Response value(@PathVariable String type, List<String> nodes) {
        switch (type) {
            case "node":
                return ok();
            case "nodes":
                return ok();
            default:
                return fail("This node is not existent");
        }
    }
}
