package com.pnoker.transfer.opc.controller;

import com.pnoker.common.base.BaseController;
import com.pnoker.common.bean.base.ResponseBean;
import lombok.extern.slf4j.Slf4j;
import org.jinterop.dcom.common.JIException;
import org.openscada.opc.dcom.list.ClassDetails;
import org.openscada.opc.lib.list.Categories;
import org.openscada.opc.lib.list.Category;
import org.openscada.opc.lib.list.ServerList;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.UnknownHostException;
import java.util.Collection;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Opc
 */
@Slf4j
@RestController
public class OpcController extends BaseController {

    @RequestMapping(value = "/list/{type}", method = RequestMethod.GET)
    public ResponseBean list(@PathVariable String type) {
        Object object;
        switch (type) {
            case "server":
                try {
                    ServerList serverList = new ServerList(
                            "localhost",
                            "Administrator",
                            "939510703",
                            "");
                    Collection<ClassDetails> classDetails = serverList.listServersWithDetails(new Category[]{
                                    Categories.OPCDAServer10,
                                    Categories.OPCDAServer20,
                                    Categories.OPCDAServer30},
                            new Category[]{});
                    for (ClassDetails cds : classDetails) {
                        System.out.println(cds.getProgId() + "=" + cds.getDescription());
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (JIException e) {
                    e.printStackTrace();
                }
                break;
            case "group":
                break;
            case "item":
                break;
            default:
                return fail("This type is not supported");
        }
        return ok(object);
    }
}
