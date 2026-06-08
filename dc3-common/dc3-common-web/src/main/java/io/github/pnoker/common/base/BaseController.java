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

package io.github.pnoker.common.base;

import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.entity.common.TenantOwned;
import io.github.pnoker.common.exception.AccessDeniedException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.security.GatewayAuthenticationToken;
import io.github.pnoker.common.security.PermissionProvider;
import io.github.pnoker.common.utils.UserHeaderUtil;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Base Controller Interface
 * <p>
 * Provides default methods for common controller operations such as extracting user
 * information from request headers in reactive applications.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface BaseController {

    /**
     * Get user header information from request context
     *
     * @return User header information as Mono
     */
    default Mono<RequestHeader.UserHeader> getUserHeader() {
        return UserHeaderUtil.getUserHeader();
    }

    /**
     * Get tenant ID from user header
     *
     * @return Tenant ID as Mono
     */
    default Mono<Long> getTenantId() {
        return UserHeaderUtil.getTenantId();
    }

    /**
     * Fail closed when an ID-based lookup returns an entity outside the caller's tenant.
     * Returning 404 avoids exposing whether a cross-tenant resource exists.
     */
    default <T extends TenantOwned> T requireTenant(Long tenantId, T entity) {
        if (Objects.isNull(entity) || !Objects.equals(tenantId, entity.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
        return entity;
    }

    /**
     * Keep only entities that belong to the caller's tenant for bulk lookup endpoints.
     */
    default <T extends TenantOwned> List<T> filterTenant(Long tenantId, Collection<T> entities) {
        if (Objects.isNull(entities) || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream()
                .filter(Objects::nonNull)
                .filter(entity -> Objects.equals(tenantId, entity.getTenantId()))
                .toList();
    }

    /**
     * Get user ID from user header
     *
     * @return User ID as Mono
     */
    default Mono<Long> getUserId() {
        return UserHeaderUtil.getUserId();
    }

    /**
     * Get user nickname from user header
     *
     * @return User nickname as Mono
     */
    default Mono<String> getNickName() {
        return UserHeaderUtil.getNickName();
    }

    /**
     * Get username from user header
     *
     * @return Username as Mono
     */
    default Mono<String> getUserName() {
        return UserHeaderUtil.getUserName();
    }

    /**
     * Run a synchronous (typically JDBC / blocking-IO) supplier on the bounded-elastic
     * scheduler so the Netty event loop stays free.
     * <p>
     * Use this in reactive controllers that wrap blocking service calls. Exceptions
     * thrown by the supplier propagate as {@code Mono.error(...)} and are mapped to
     * {@code R.fail(...)} by the global {@code ExceptionConfig}, so callers should not
     * try/catch around the supplier.
     */
    default <T> Mono<R<T>> async(Supplier<R<T>> supplier) {
        return Mono.fromCallable(supplier::get).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Assert that the current user holds at least one of the given resource permissions.
     * <p>
     * Checks the Spring Security context first (authority set loaded at authentication
     * time).  Falls back to {@link PermissionProvider#hasPermission} only when no
     * {@link GatewayAuthenticationToken} is present in the context — for example during
     * tests or in modules that do not include Spring Security.
     *
     * @param provider      PermissionProvider (injected by caller)
     * @param resourceCodes resource permission codes to check
     * @return Mono that completes empty on success, errors on denial
     */
    default Mono<Void> requireAnyPermission(PermissionProvider provider, String... resourceCodes) {
        Set<String> required = Arrays.stream(resourceCodes)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (required.isEmpty()) {
            return Mono.empty();
        }

        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(auth -> auth instanceof GatewayAuthenticationToken)
                .cast(GatewayAuthenticationToken.class)
                .flatMap(token -> {
                    Set<String> authorities = token.getAuthorities().stream()
                            .map(a -> a.getAuthority())
                            .collect(Collectors.toSet());
                    boolean granted = required.stream().anyMatch(authorities::contains);
                    if (granted) {
                        return Mono.<Void>empty();
                    }
                    return Mono.error(new AccessDeniedException(
                            "Access denied: none of the required permissions are granted"));
                })
                // Fallback: SecurityContext not available — use provider directly
                .switchIfEmpty(Mono.defer(() ->
                        getTenantId().flatMap(tenantId ->
                                getUserId().flatMap(userId ->
                                        Flux.fromIterable(required)
                                                .flatMap(code -> provider.hasPermission(tenantId, userId, code))
                                                .any(granted -> granted)
                                                .flatMap(hasPermission -> {
                                                    if (Boolean.TRUE.equals(hasPermission)) {
                                                        return Mono.empty();
                                                    }
                                                    return Mono.error(new AccessDeniedException(
                                                            "Access denied: none of the required permissions are granted"));
                                                })
                                )
                        )));
    }

}
