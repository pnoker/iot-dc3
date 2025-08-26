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

package io.github.pnoker.common.strategy;

import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.constant.driver.StrategyConstant;
import io.github.pnoker.common.repository.RepositoryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据存储策略工厂
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public class RepositoryStrategyFactory {

    private static final Map<String, RepositoryService> savingStrategyServiceMap = new ConcurrentHashMap<>();

    private RepositoryStrategyFactory() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    public static List<RepositoryService> get() {
        return new ArrayList<>(savingStrategyServiceMap.values());
    }

    public static RepositoryService get(String name) {
        return savingStrategyServiceMap.get(StrategyConstant.Storage.REPOSITORY_PREFIX + name);
    }

    public static void put(String name, RepositoryService service) {
        savingStrategyServiceMap.put(StrategyConstant.Storage.REPOSITORY_PREFIX + name, service);
    }
}
