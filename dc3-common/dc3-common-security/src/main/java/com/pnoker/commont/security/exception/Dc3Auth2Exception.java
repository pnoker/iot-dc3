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

package com.pnoker.commont.security.exception;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.pnoker.commont.security.component.Dc3Auth2ExceptionSerializer;
import lombok.Getter;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

/**
 * 自定义OAuth2Exception
 * <p>
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@JsonSerialize(using = Dc3Auth2ExceptionSerializer.class)
public class Dc3Auth2Exception extends OAuth2Exception {
    @Getter
    private String errorCode;

    public Dc3Auth2Exception(String msg) {
        super(msg);
    }

    public Dc3Auth2Exception(String msg, String errorCode) {
        super(msg);
        this.errorCode = errorCode;
    }
}
