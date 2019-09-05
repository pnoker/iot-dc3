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

package com.pnoker.center.collect;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

/**
 * @Author: lyang
 * @Date: 2019/12/29 16:35
 */
@Component
@Slf4j
@EnableBinding(MyProcessor.class)
public class MyMQReciver {

    @StreamListener(MyProcessor.INPUT)
    @SendTo(MyProcessor.CALLBACKINPUT)
    public String process(MyGirl myGirl){
        log.info("collect comming : {} ", myGirl.toString());
        return myGirl.toString();
    }

    @StreamListener(MyProcessor.CALLBACKINPUT)
    public void callback(String myGirl){
        log.info("collect has recived : {} ", myGirl);
    }
}
