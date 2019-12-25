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

package com.pnoker.common.model.auth;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.pnoker.common.constant.Common;
import com.pnoker.common.model.Description;
import com.pnoker.common.utils.KeyUtil;
import com.pnoker.common.valid.Auth;
import com.pnoker.common.valid.Insert;
import com.pnoker.common.valid.Update;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Calendar;
import java.util.Date;

/**
 * <p>Token
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Data
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class Token extends Description {

    @NotNull(message = "user id can't be empty", groups = {Insert.class, Update.class, Auth.class})
    private Long userId;

    @NotBlank(message = "token can't be empty", groups = {Auth.class})
    private String token;
    private String privateKey;

    @JsonFormat(pattern = Common.DATEFORMAT, timezone = Common.TIMEZONE)
    private Date expireTime;


    /**
     * 是否重新生成 Key
     */
    @TableField(exist = false)
    private boolean newKey = false;

    /**
     * Token 持续时间，单位：小时
     */
    @TableField(exist = false)
    private int duration = 0;

    public Token() {
        generate(true, 6);
        super.setCreateTime(new Date());
    }

    @SneakyThrows
    public Token generate(boolean isNewKey, int duration) {
        expireTime(duration);
        if (isNewKey) {
            this.privateKey = KeyUtil.genAesKey().getPrivateKey();
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append(this.expireTime.getTime()).append("|").append(this.userId).append("|").append(this.privateKey);
        this.token = KeyUtil.encryptAes(buffer.toString(), this.privateKey);
        return this;
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
