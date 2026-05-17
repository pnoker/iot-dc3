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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data Storage Strategy Factory Used to manage and store different repository services
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public class RepositoryStrategyFactory {

    // Map to store repository services with name as key
    private static final Map<String, RepositoryService> repositoryServiceMap = new ConcurrentHashMap<>();

    // Private constructor to prevent instantiation
    private RepositoryStrategyFactory() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Get all registered repository services
     *
     * @return List of all repository services
     */
    public static List<RepositoryService> get() {
        return new ArrayList<>(repositoryServiceMap.values());
    }

    /**
     * Get repository service by name
     *
     * @param name Name of the repository service
     * @return RepositoryService instance for the given name
     */
    public static RepositoryService get(String name) {
        String key = repositoryKey(name);
        if (Objects.isNull(key)) {
            return null;
        }
        return repositoryServiceMap.get(key);
    }

    /**
     * Register a repository service with its declared repository name.
     *
     * @param service RepositoryService instance to register
     */
    public static void put(RepositoryService service) {
        Objects.requireNonNull(service, "Repository service must not be null");
        put(service.getRepositoryName(), service);
    }

    /**
     * Register a new repository service
     *
     * @param name    Name of the repository service
     * @param service RepositoryService instance to register
     */
    public static void put(String name, RepositoryService service) {
        Objects.requireNonNull(service, "Repository service must not be null");
        String key = repositoryKey(name);
        if (Objects.isNull(key)) {
            throw new IllegalArgumentException("Repository strategy name must not be blank");
        }
        repositoryServiceMap.put(key, service);
    }

    /**
     * Remove repository service by name.
     *
     * @param name Name of the repository service
     */
    public static void remove(String name) {
        String key = repositoryKey(name);
        if (Objects.isNull(key)) {
            return;
        }
        repositoryServiceMap.remove(key);
    }

    /**
     * Clear all registered repository services.
     */
    public static void clear() {
        repositoryServiceMap.clear();
    }

    private static String repositoryKey(String name) {
        String repositoryName = StringUtils.trimToNull(name);
        return Objects.isNull(repositoryName) ? null : StrategyConstant.Storage.REPOSITORY_PREFIX + repositoryName;
    }

}
