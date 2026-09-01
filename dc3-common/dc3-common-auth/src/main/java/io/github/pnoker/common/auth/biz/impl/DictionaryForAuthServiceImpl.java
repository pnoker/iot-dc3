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

package io.github.pnoker.common.auth.biz.impl;

import io.github.pnoker.common.auth.biz.DictionaryForAuthService;
import io.github.pnoker.common.auth.repository.ReactiveTenantDictionaryStore;
import io.github.pnoker.common.entity.option.DictionaryOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Dictionary lookup service implementation for the auth module.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictionaryForAuthServiceImpl implements DictionaryForAuthService {

    private final ReactiveTenantDictionaryStore tenantStore;

    @Override
    public Mono<List<DictionaryOption>> listTenantOptions() {
        return tenantStore.listEnabled()
                .map(tenant -> DictionaryOption.leaf(tenant.getTenantName(), tenant.getId().toString()))
                .collectList();
    }

}
