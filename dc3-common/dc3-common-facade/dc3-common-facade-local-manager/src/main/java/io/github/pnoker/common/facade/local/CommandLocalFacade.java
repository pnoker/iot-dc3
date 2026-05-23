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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.facade.api.CommandFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeCommandQuery;
import io.github.pnoker.common.facade.local.builder.FacadeCommandBuilder;
import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.entity.query.CommandQuery;
import io.github.pnoker.common.manager.service.CommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * In-process CommandFacade implementation.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandLocalFacade implements CommandFacade {

    private final CommandService commandService;

    private final FacadeCommandBuilder facadeCommandBuilder;

    @Override
    public FacadeCommandBO getById(Long id) {
        CommandBO managerBO = commandService.getById(id);
        return Objects.isNull(managerBO) ? null : facadeCommandBuilder.toFacadeBO(managerBO);
    }

    @Override
    public List<FacadeCommandBO> listByIds(Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<CommandBO> list = commandService.listByIds(new HashSet<>(ids));
        if (Objects.isNull(list) || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(facadeCommandBuilder::toFacadeBO).toList();
    }

    @Override
    public FacadePage<FacadeCommandBO> listByPage(FacadeCommandQuery query) {
        CommandQuery managerQuery = facadeCommandBuilder.toManagerQuery(query);
        Page<CommandBO> page = commandService.list(managerQuery);
        if (Objects.isNull(page)) {
            return FacadePage.empty();
        }

        List<FacadeCommandBO> records = page.getRecords().stream().map(facadeCommandBuilder::toFacadeBO).toList();
        return new FacadePage<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getPages(), records);
    }

}
