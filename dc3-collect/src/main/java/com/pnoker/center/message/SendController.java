package com.pnoker.center.message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: lyang
 * @Date: 2018/12/29 16:34
 */
@RestController
public class SendController {

    @Autowired
    private MyProcessor processor;

    @PostMapping("/send")
    public String send(@RequestBody MyGirl myGirl ){
        processor.output().send(MessageBuilder.withPayload(myGirl).build());
        return "send a message to my girl";
    }
}
