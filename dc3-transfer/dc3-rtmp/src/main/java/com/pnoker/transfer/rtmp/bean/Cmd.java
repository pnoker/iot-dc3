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
 * <p>Description:
 */
@Data
public class Cmd {
    private String cmd;
    private StringBuilder builder;

    public Cmd(String path) {
        this.builder = new StringBuilder(path);
    }

    public Cmd create(String exe) {
        if (null != builder) {
            builder.append(exe);
        }
        return this;
    }

    public Cmd add(String cmd) {
        if (null != builder) {
            builder.append(" " + cmd);
        }
        return this;
    }

    public Cmd add(String key, String cmd) {
        return add(key).add(cmd);
    }

    public void build() {
        cmd = builder.toString();
    }
}
