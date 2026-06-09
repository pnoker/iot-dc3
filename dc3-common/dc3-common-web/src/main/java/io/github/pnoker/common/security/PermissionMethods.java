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

package io.github.pnoker.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * SpEL-accessible permission check methods for use in {@code @PreAuthorize}.
 * <p>
 * Usage in controllers:
 * <pre>{@code
 *   @PreAuthorize("@perm.can('device', 'get')")
 *   @GetMapping("/{id}")
 *   public Mono<R<DeviceVO>> getById(@PathVariable Long id) { ... }
 * }</pre>
 * <p>
 * The bean composes the full {@code resource_code} as
 * {@code {spring.application.name}:{domain}:{scope}} and checks it against the
 * caller's authority set loaded at authentication time.
 *
 * @author pnoker
 * @version 2026.6.0
 * @since 2016.10.1
 */
@Slf4j
@Component("perm")
public class PermissionMethods {

    private final String serviceName;

    public PermissionMethods(@Value("${spring.application.name:unknown}") String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * Check whether the current user holds the permission
     * {@code {spring.application.name}:{domain}:{scope}}.
     *
     * @param domain domain identifier (e.g. "device", "driver", "point")
     * @param scope  operation scope (e.g. "get", "search", "add", "update", "delete")
     * @return true if granted
     */
    /**
     * Wildcard authority granted by the permissive {@code DefaultPermissionProvider}
     * (services without the auth module). When present it satisfies every check, so
     * service-to-service trust works without role data.
     */
    public static final String WILDCARD = "*";

    public boolean can(String domain, String scope) {
        String resourceCode = serviceName + ":" + domain + ":" + scope;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        Set<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        boolean granted = authorities.contains(WILDCARD) || authorities.contains(resourceCode);
        if (!granted && log.isDebugEnabled()) {
            log.debug("Permission check failed: required={}, user has={}", resourceCode, authorities);
        }
        return granted;
    }

    /**
     * Check whether the current user holds at least one of the given permissions.
     * Each spec is a {@code domain:scope} pair.  The service name is prepended
     * automatically.
     *
     * @param specs varargs of {@code domain:scope} strings
     * @return true if at least one is granted
     */
    public boolean any(String... specs) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        Set<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        if (authorities.contains(WILDCARD)) {
            return true;
        }
        for (String spec : specs) {
            String resourceCode = serviceName + ":" + spec;
            if (authorities.contains(resourceCode)) {
                return true;
            }
        }
        return false;
    }
}
