package com.pnoker.transfer.resource.controller;

import com.pnoker.common.base.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: 主控制器类
 */
@Slf4j
@Controller
@RequestMapping("/")
public class MainController extends BaseController {

    @RequestMapping(value = "/video/{name}", method = RequestMethod.GET)
    public String videoView(ModelMap modelMap, @PathVariable String name) {
        modelMap.addAttribute("name", name);
        return "mp4";
    }

}
