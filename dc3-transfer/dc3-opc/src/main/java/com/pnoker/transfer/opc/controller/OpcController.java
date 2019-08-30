package com.pnoker.transfer.opc.controller;

import com.pnoker.common.base.BaseController;
import lombok.extern.slf4j.Slf4j;
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

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public String list() {

        return list;
    }
}
