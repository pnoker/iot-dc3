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
package io.github.pnoker.common.agentic.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.pnoker.common.exception.RequestException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Validates user-supplied model provider base URLs before the server connects
 * to them, closing the connectivity-check SSRF channel: only http(s) URLs whose
 * host resolves exclusively to public addresses are accepted.
 *
 * <p>Every address the host resolves to must pass — a hostname mixing a public
 * and a private address is rejected, so a DNS record cannot smuggle a probe
 * (or a stored API key) onto an internal network. Resolution verdicts are
 * cached with a short TTL so a hostname cannot be validated once with a clean
 * record and then immediately re-resolved to an internal address by the actual
 * outbound call.
 *
 * <p>This deliberately blocks internal LLM gateways (localhost, private
 * networks); there is no allowlist escape hatch by design. Deployments that
 * need an internal model endpoint must expose it through a public resolvable
 * hostname.
 *
 * @author pnoker
 * @since 2026.10.10
 */
@Slf4j
@Component
public class AgenticBaseUrlValidator {

    private static final int RESOLUTION_CACHE_MAXIMUM_SIZE = 1024;
    private static final Duration RESOLUTION_CACHE_TTL = Duration.ofSeconds(30);

    /** DNS resolver seam; the production constructor wires {@code InetAddress::getAllByName}. */
    private final DnsResolver resolver;

    /**
     * Whether plain http base URLs are rejected on top of the address rules. Defaults
     * to true in the Spring context: provider API keys must not cross the network in
     * clear text. Explicitly constructed instances (tests) keep the lenient default
     * so the address rules stay independently testable.
     */
    @org.springframework.beans.factory.annotation.Value("${dc3.agentic.security-base-url-https-only:true}")
    private boolean httpsOnly;

    private final Cache<String, Boolean> resolutionVerdicts = Caffeine.newBuilder()
            .maximumSize(RESOLUTION_CACHE_MAXIMUM_SIZE)
            .expireAfterWrite(RESOLUTION_CACHE_TTL)
            .build();

    /** Production constructor using the platform DNS resolver. */
    @Autowired
    public AgenticBaseUrlValidator() {
        this(InetAddress::getAllByName);
    }

    /** Constructor with an injectable resolver (test seam); https-only stays off. */
    public AgenticBaseUrlValidator(DnsResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Constructor with an injectable resolver and an explicit https-only policy.
     *
     * @param resolver  injectable resolver (test seam)
     * @param httpsOnly whether plain http base URLs are rejected
     */
    public AgenticBaseUrlValidator(DnsResolver resolver, boolean httpsOnly) {
        this.resolver = resolver;
        this.httpsOnly = httpsOnly;
    }

    /** Resolver contract allowing the checked {@link UnknownHostException}. */
    @FunctionalInterface
    public interface DnsResolver {

        /**
         * Resolve a host name to all its addresses.
         *
         * @param host host name
         * @return resolved addresses, never null
         * @throws UnknownHostException when the host cannot be resolved
         */
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    /**
     * Validate a base URL reactively: completes empty when the URL is an
     * http(s) URL pointing at a public host, errors with a
     * {@link RequestException} otherwise. DNS resolution runs on the
     * bounded-elastic scheduler so callers never block an event loop.
     *
     * @param baseUrl base URL to validate, may be null or blank
     * @return empty mono on success, error mono with a rejection reason
     */
    public Mono<Void> validateReactive(String baseUrl) {
        return Mono.fromCallable(() -> {
                    validate(baseUrl);
                    return true;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * Validate a base URL synchronously. Blocking (DNS) — callers on an event
     * loop must use {@link #validateReactive(String)}.
     *
     * @param baseUrl base URL to validate, may be null or blank
     * @throws RequestException when the scheme is not http(s), the URL is
     *                          malformed, or the host resolves (wholly or
     *                          partly) to a non-public address
     */
    public void validate(String baseUrl) {
        URI uri = parse(baseUrl);
        String host = uri.getHost();
        Boolean reachable = resolutionVerdicts.get(host, this::hostIsPublic);
        if (!Boolean.TRUE.equals(reachable)) {
            throw new RequestException(
                    "Provider base URL is not allowed: host '{}' does not resolve to a public address", host);
        }
    }

    private URI parse(String baseUrl) {
        if (StringUtils.isBlank(baseUrl)) {
            throw new RequestException("Provider base URL is required");
        }
        URI uri;
        try {
            uri = URI.create(baseUrl.trim());
        } catch (IllegalArgumentException exception) {
            throw new RequestException("Provider base URL is malformed: {}", exception.getMessage());
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new RequestException("Provider base URL must use the http or https scheme");
        }
        if ("http".equalsIgnoreCase(scheme) && httpsOnly) {
            throw new RequestException(
                    "Provider base URL must use https; set dc3.agentic.security-base-url-https-only=false only for "
                            + "trusted plain-http deployments");
        }
        if (StringUtils.isBlank(uri.getHost())) {
            throw new RequestException("Provider base URL must carry a host name");
        }
        if (uri.getUserInfo() != null) {
            throw new RequestException("Provider base URL must not carry user info");
        }
        return uri;
    }

    /**
     * Whether every address the host resolves to is public. Unresolvable
     * hosts are rejected (treated as not public) rather than raising, so a
     * typo cannot be distinguished from a blocked target by the caller.
     */
    private boolean hostIsPublic(String host) {
        InetAddress[] addresses;
        try {
            addresses = resolver.resolve(host);
        } catch (UnknownHostException exception) {
            return false;
        }
        if (addresses == null || addresses.length == 0) {
            return false;
        }
        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether the address sits in a range the platform must never be tricked
     * into probing: loopback, link-local, site-local/private, unique-local
     * IPv6, any/multicast/reserved IPv4.
     */
    private static boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        if (address instanceof Inet6Address) {
            return isBlockedIpv6((Inet6Address) address);
        }
        if (address instanceof Inet4Address) {
            return isBlockedIpv4((Inet4Address) address);
        }
        return true;
    }

    private static boolean isBlockedIpv6(Inet6Address address) {
        byte[] bytes = address.getAddress();
        // Unique-local fc00::/7 (site-local fe80::/10 is covered by isLinkLocalAddress).
        if ((bytes[0] & 0xFE) == 0xFC) return true;
        // NAT64 64:ff9b::/96 maps straight back into IPv4 space (including private).
        if (bytes[0] == 0x00 && bytes[1] == 0x64 && bytes[2] == (byte) 0xFF && bytes[3] == (byte) 0x9B) return true;
        // 6to4 2002::/16 embeds an arbitrary IPv4 address.
        if (bytes[0] == 0x20 && bytes[1] == 0x02) return true;
        // IPv4-mapped ::ffff:0:0/96 for resolvers that skip JDK normalization.
        if (bytes[10] == (byte) 0xFF && bytes[11] == (byte) 0xFF) return true;
        return false;
    }

    private static boolean isBlockedIpv4(Inet4Address address) {
        byte[] b = address.getAddress();
        int b0 = b[0] & 0xFF;
        int b1 = b[1] & 0xFF;
        // 0.0.0.0/8 "this" network (0.0.0.0 itself is isAnyLocalAddress)
        if (b0 == 0) return true;
        // 10.0.0.0/8 private
        if (b0 == 10) return true;
        // 100.64.0.0/10 carrier-grade NAT
        if (b0 == 100 && (b1 & 0xC0) == 0x40) return true;
        // 127.0.0.0/8 loopback (isLoopbackAddress covers it; kept explicit)
        if (b0 == 127) return true;
        // 169.254.0.0/16 link-local
        if (b0 == 169 && b1 == 254) return true;
        // 172.16.0.0/12 private
        if (b0 == 172 && (b1 & 0xF0) == 16) return true;
        // 192.0.0.0/24 and 192.0.2.0/24 (TEST-NET-1) special purpose
        if (b0 == 192 && b1 == 0 && (b[2] & 0xFF) == 0) return true;
        if (b0 == 192 && b1 == 0 && (b[2] & 0xFF) == 2) return true;
        // 192.88.99.0/24 6to4 relay
        if (b0 == 192 && b1 == 88 && (b[2] & 0xFF) == 99) return true;
        // 192.168.0.0/16 private
        if (b0 == 192 && b1 == 168) return true;
        // 198.18.0.0/15 benchmarking
        if (b0 == 198 && (b1 == 18 || b1 == 19)) return true;
        // 198.51.100.0/24 (TEST-NET-2) and 203.0.113.0/24 (TEST-NET-3) documentation
        if (b0 == 198 && b1 == 51 && (b[2] & 0xFF) == 100) return true;
        if (b0 == 203 && b1 == 0 && (b[2] & 0xFF) == 113) return true;
        // 240.0.0.0/4 reserved (broadcast 255.255.255.255 included)
        return (b0 & 0xF0) == 0xF0;
    }
}
