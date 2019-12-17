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

package com.pnoker.common.entity.auth;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.pnoker.common.constant.Common;
import com.pnoker.common.entity.Description;
import com.pnoker.common.tool.AesTools;
import lombok.*;
import lombok.experimental.Accessors;

import javax.validation.constraints.Future;
import java.util.Calendar;
import java.util.Date;

/**
 * <p>Token
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class Token extends Description {

    private String token;
    private String privateKey;

    @JsonFormat(pattern = Common.DATEFORMAT, timezone = Common.TIMEZONE)
    @Future(message = "expire time must be greater than the current time")
    private Date expireTime;

    @SneakyThrows
    public Token(int hour) {
        this.token = IdUtil.simpleUUID();
        this.privateKey = AesTools.genKey().getPrivateKey();
        expireTime(hour);
        super.setCreateTime(new Date());
    }

    public Token expireTime(int hour) {
        hour = hour < 1 ? 6 : hour;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR, hour);
        expireTime = calendar.getTime();
        return this;
    }

}
