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
import io.github.pnoker.common.facade.api.ProfileFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeProfileBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeProfileQuery;
import io.github.pnoker.common.facade.local.builder.FacadeProfileBuilder;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.entity.query.ProfileQuery;
import io.github.pnoker.common.manager.service.ProfileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * In-process ProfileFacade implementation.
 *
 * @author pnoker
 * @version 2026.5.14
 * @since 2016.10.1
 */
@Slf4j
@Component
public class ProfileLocalFacade implements ProfileFacade {

    @Resource
    private ProfileService profileService;

    @Resource
    private FacadeProfileBuilder facadeProfileBuilder;

    @Override
    public FacadeProfileBO selectById(Long id) {
        ProfileBO managerBO = profileService.selectById(id);
        return Objects.isNull(managerBO) ? null : facadeProfileBuilder.toFacadeBO(managerBO);
    }

    @Override
    public List<FacadeProfileBO> selectByIds(Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProfileBO> list = profileService.selectByIds(new HashSet<>(ids));
        if (Objects.isNull(list) || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(facadeProfileBuilder::toFacadeBO).toList();
    }

    @Override
    public FacadePage<FacadeProfileBO> selectByPage(FacadeProfileQuery query) {
        ProfileQuery managerQuery = facadeProfileBuilder.toManagerQuery(query);
        Page<ProfileBO> page = profileService.selectByPage(managerQuery);
        if (Objects.isNull(page)) {
            return FacadePage.empty();
        }

        List<FacadeProfileBO> records = page.getRecords().stream().map(facadeProfileBuilder::toFacadeBO).toList();
        return new FacadePage<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getPages(), records);
    }

    @Override
    public List<FacadeProfileBO> selectByDeviceId(Long deviceId) {
        List<ProfileBO> list = profileService.selectByDeviceId(deviceId);
        if (Objects.isNull(list) || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(facadeProfileBuilder::toFacadeBO).toList();
    }

}
