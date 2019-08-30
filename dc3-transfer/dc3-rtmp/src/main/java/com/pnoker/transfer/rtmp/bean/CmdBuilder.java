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
package com.pnoker.transfer.rtmp.bean;

import lombok.Data;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Command 指令构造器
 */
@Data
public class CmdBuilder {
    private String cmd;
    private StringBuilder builder;

    /**
     * 构造函数,传指令程序基地址
     *
     * @param path
     */
    public CmdBuilder(String path) {
        this.builder = new StringBuilder(path);
    }

    /**
     * 可运行程序名称,填写全称
     *
     * @param exe
     * @return
     */
    public CmdBuilder create(String exe) {
        if (null != builder) {
            builder.append(exe);
        }
        return this;
    }

    /**
     * 键值对指令模式
     *
     * @param key
     * @param paramer
     * @return
     */
    public CmdBuilder add(String key, String paramer) {
        return add(key).add(paramer);
    }

    /**
     * 单个指令模式
     *
     * @param paramer
     * @return
     */
    public CmdBuilder add(String paramer) {
        if (null != builder) {
            builder.append(" ");
            builder.append(paramer);
        }
        return this;
    }

    public void build() {
        cmd = builder.toString();
    }
}
