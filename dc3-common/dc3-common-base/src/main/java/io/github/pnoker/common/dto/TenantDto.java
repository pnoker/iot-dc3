/*
 * Copyright 2022 Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.dto;

import io.github.pnoker.common.base.Converter;
import io.github.pnoker.common.bean.Pages;
import io.github.pnoker.common.model.Tenant;
import lombok.*;
import org.springframework.beans.BeanUtils;

/**
 * User DTO
 *
 * @author pnoker
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TenantDto extends Tenant implements Converter<Tenant, TenantDto> {

    private Pages page;

    @Override
    public void convertDtoToDo(Tenant tenant) {
        BeanUtils.copyProperties(this, tenant);
    }

    @Override
    public void convertDoToDto(Tenant tenant) {
        BeanUtils.copyProperties(tenant, this);
    }
}