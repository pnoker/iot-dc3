package com.pnoker.transfer.message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SendController {

    @Autowired
    private Processor pipe;

    @GetMapping("/send")
    public void send(@RequestParam String message) {
        pipe.output().send(MessageBuilder.withPayload(message).build());
    }
}