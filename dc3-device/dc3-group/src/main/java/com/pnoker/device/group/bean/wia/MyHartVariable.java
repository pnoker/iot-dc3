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
package com.pnoker.device.group.bean.wia;

import lombok.Data;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Wia Hart 数据信息
 */
@Data
public class MyHartVariable {
    private long variableId;
    private String variableName;
    private float value;
    private long time;

    public MyHartVariable(String variableName) {
        this.variableName = variableName;
        this.time = System.currentTimeMillis();
    }

    /**
     * 更新数据，并触发相应操作
     *
     * @param value
     */
    public void update(float value) {
        this.value = value;
        this.time = System.currentTimeMillis();
        //发送消息队列
        //入库
    }
}
