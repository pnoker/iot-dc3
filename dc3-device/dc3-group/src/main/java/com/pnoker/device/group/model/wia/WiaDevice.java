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

package com.pnoker.device.group.model.wia;

import lombok.Data;

/**
 * <p>设备组 Wia设备表
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Data
public class WiaDevice {
    private long id;
    private long gatewayId;
    private String name;
    private String code;
    private String longAddress;
    private String type;
    private String location;
    private int status;
    private long time;

}
