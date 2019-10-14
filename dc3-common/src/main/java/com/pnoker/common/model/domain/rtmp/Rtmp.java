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

package com.pnoker.common.model.domain.rtmp;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pnoker.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>Rtmp 信息实体类
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rtmp extends BaseEntity {
    private static final long serialVersionUID = 594764013253279856L;

    private String name;
    private String rtspUrl;
    private String rtmpUrl;
    private String command;
    private short videoType;
    private Boolean autoStart;
    private Long nodeId;
    private Long imageId;

    public Rtmp(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public void query(QueryWrapper<Rtmp> queryWrapper) {
        if (autoStart != null) {
            if (autoStart) {
                queryWrapper.eq("auto_start", true);
            } else {
                queryWrapper.eq("auto_start", false);
            }
        }
        if (null != name && !"".equals(name)) {
            queryWrapper.like("name", name);
        }
    }
}