/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
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

package com.dc3.common.model;

import com.dc3.common.valid.Insert;
import lombok.*;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;

/**
 * Rtmp
 *
 * @author pnoker
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Rtmp extends Description {

    @NotBlank(message = "name can't be empty", groups = {Insert.class})
    private String name;

    @NotBlank(message = "rtsp url can't be empty", groups = {Insert.class})
    private String rtspUrl;

    @NotBlank(message = "rtmp url can't be empty", groups = {Insert.class})
    private String rtmpUrl;

    @NotBlank(message = "command can't be empty", groups = {Insert.class})
    private String command;

    private Short videoType;
    private Boolean run;
    private Boolean autoStart;

    public Rtmp(long id, boolean run) {
        super.setId(id);
        this.run = run;
    }

    public Rtmp(boolean autoStart) {
        this.autoStart = autoStart;
    }

}