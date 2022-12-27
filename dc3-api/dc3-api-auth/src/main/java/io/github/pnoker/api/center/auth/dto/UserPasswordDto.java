/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.api.center.auth.dto;

import io.github.pnoker.common.base.Converter;
import io.github.pnoker.common.bean.common.Pages;
import io.github.pnoker.common.entity.UserPassword;
import lombok.*;
import org.springframework.beans.BeanUtils;

/**
 * UserPassword DTO
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class UserPasswordDto extends UserPassword implements Converter<UserPassword> {

    private Pages page;

    @Override
    public void convertDtoToDo(UserPassword userPassword) {
        BeanUtils.copyProperties(this, userPassword);
    }

    @Override
    public void convertDoToDto(UserPassword userPassword) {
        BeanUtils.copyProperties(userPassword, this);
    }
}