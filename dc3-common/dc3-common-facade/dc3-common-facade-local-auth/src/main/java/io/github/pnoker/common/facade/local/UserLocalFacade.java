/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.service.UserService;
import io.github.pnoker.common.facade.api.UserFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeUserBO;
import io.github.pnoker.common.facade.local.builder.FacadeUserBuilder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * In-process {@link UserFacade}.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.5
 */
@Slf4j
@Component
public class UserLocalFacade implements UserFacade {

    @Resource
    private UserService userService;

    @Resource
    private FacadeUserBuilder facadeUserBuilder;

    @Override
    public FacadeUserBO selectById(Long id) {
        UserBO bo = userService.selectById(id);
        return Objects.isNull(bo) ? null : facadeUserBuilder.toFacadeBO(bo);
    }

}
