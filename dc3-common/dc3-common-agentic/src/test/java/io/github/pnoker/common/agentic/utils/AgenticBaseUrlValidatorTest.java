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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.pnoker.common.exception.RequestException;
import java.net.InetAddress;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

/**
 * SSRF regression tests for the base URL validator: loopback, private,
 * link-local and reserved targets are rejected, multi-address DNS answers
 * with any non-public address are rejected (DNS rebinding), and only
 * http(s) URLs to public hosts pass.
 */
class AgenticBaseUrlValidatorTest {

    private static InetAddress address(String literal) {
        try {
            return InetAddress.getByName(literal);
        } catch (java.net.UnknownHostException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static AgenticBaseUrlValidator validatorReturning(InetAddress... addresses) {
        return new AgenticBaseUrlValidator(host -> addresses);
    }

    private static AgenticBaseUrlValidator validatorReturning(boolean httpsOnly, InetAddress... addresses) {
        return new AgenticBaseUrlValidator(host -> addresses, httpsOnly);
    }

    @Test
    void publicHttpUrlIsAccepted() {
        AgenticBaseUrlValidator validator = validatorReturning(address("93.184.216.34"));
        assertThatCode(() -> validator.validate("https://api.example.com/v1")).doesNotThrowAnyException();
        // The lenient (test-seam) constructor keeps plain http allowed so the address
        // rules stay independently testable.
        assertThatCode(() -> validator.validate("http://api.example.com:8080/v1"))
                .doesNotThrowAnyException();
    }

    @Test
    void plainHttpIsRejectedWhenHttpsOnlyIsEnabled() {
        AgenticBaseUrlValidator validator = validatorReturning(true, address("93.184.216.34"));
        assertThatThrownBy(() -> validator.validate("http://api.example.com:8080/v1"))
                .isInstanceOf(RequestException.class)
                .hasMessageContaining("https");
        assertThatCode(() -> validator.validate("https://api.example.com/v1")).doesNotThrowAnyException();
    }

    @Test
    void nat64And6to4AndIpv4MappedAddressesAreRejected() {
        // NAT64 64:ff9b::/96 embeds 10.1.2.3 — must not tunnel past the IPv4 rules.
        assertThatThrownBy(() -> validatorReturning(address("64:ff9b::a01:203"))
                        .validate("https://nat64.example.com/v1"))
                .isInstanceOf(RequestException.class);
        // 6to4 2002:c000:203:: embeds 192.0.2.3.
        assertThatThrownBy(() -> validatorReturning(address("2002:c000:203::1"))
                        .validate("https://sixtofour.example.com/v1"))
                .isInstanceOf(RequestException.class);
        // IPv4-mapped ::ffff:10.1.2.3 for resolvers that skip JDK normalization.
        assertThatThrownBy(() -> validatorReturning(address("::ffff:a01:203"))
                        .validate("https://mapped.example.com/v1"))
                .isInstanceOf(RequestException.class);
    }

    @Test
    void pinnedDnsResolvesOnlyTheValidatedAddresses() {
        InetAddress publicAddress = address("93.184.216.34");
        AgenticBaseUrlValidator validator = validatorReturning(publicAddress);
        java.util.function.Function<String, java.util.List<InetAddress>> pinned =
                validator.pinnedDns("https://api.example.com/v1");
        // The pinned resolver always answers with exactly the validated addresses,
        // so the connection cannot be re-resolved to a different host between
        // validation and connect (DNS rebinding window).
        assertThat(pinned.apply("api.example.com")).containsExactly(publicAddress);
        assertThat(pinned.apply("anything.else.example.org")).containsExactly(publicAddress);
    }

    @Test
    void pinnedDnsRejectsHostsThatFailValidation() {
        AgenticBaseUrlValidator validator = validatorReturning(address("10.0.0.8"));
        assertThatThrownBy(() -> validator.pinnedDns("https://internal.example.com/v1"))
                .isInstanceOf(RequestException.class);
    }

    @Test
    void nonHttpSchemesAreRejected() {
        AgenticBaseUrlValidator validator = validatorReturning(address("93.184.216.34"));
        assertThatThrownBy(() -> validator.validate("ftp://api.example.com"))
                .isInstanceOf(RequestException.class)
                .hasMessageContaining("http or https");
        assertThatThrownBy(() -> validator.validate("file:///etc/passwd"))
                .isInstanceOf(RequestException.class)
                .hasMessageContaining("http or https");
    }

    @Test
    void malformedAndHostlessUrlsAreRejected() {
        AgenticBaseUrlValidator validator = validatorReturning(address("93.184.216.34"));
        assertThatThrownBy(() -> validator.validate("  ")).isInstanceOf(RequestException.class);
        assertThatThrownBy(() -> validator.validate("https://")).isInstanceOf(RequestException.class);
        assertThatThrownBy(() -> validator.validate("https://user:pass@api.example.com"))
                .isInstanceOf(RequestException.class)
                .hasMessageContaining("user info");
    }

    @Test
    void loopbackTargetsAreRejected() {
        AgenticBaseUrlValidator validator = validatorReturning(address("127.0.0.1"));
        assertThatThrownBy(() -> validator.validate("http://localhost:11434/v1"))
                .isInstanceOf(RequestException.class)
                .hasMessageContaining("not allowed");
        assertThatThrownBy(() -> validator.validate("http://127.0.0.1:8080"))
                .isInstanceOf(RequestException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void privateNetworkTargetsAreRejected() {
        AgenticBaseUrlValidator validator = validatorReturning(address("10.1.2.3"));
        assertThatThrownBy(() -> validator.validate("http://10.1.2.3/v1")).isInstanceOf(RequestException.class);

        assertThatThrownBy(() -> validatorReturning(address("192.168.1.5")).validate("http://192.168.1.5/v1"))
                .isInstanceOf(RequestException.class);

        assertThatThrownBy(() -> validatorReturning(address("172.16.0.9")).validate("http://172.16.0.9/v1"))
                .isInstanceOf(RequestException.class);
    }

    @Test
    void linkLocalAndReservedTargetsAreRejected() {
        assertThatThrownBy(() -> validatorReturning(address("169.254.1.1")).validate("http://169.254.1.1"))
                .isInstanceOf(RequestException.class);
        assertThatThrownBy(() -> validatorReturning(address("192.0.2.10")).validate("http://192.0.2.10"))
                .isInstanceOf(RequestException.class);
        assertThatThrownBy(() -> validatorReturning(address("100.64.0.1")).validate("http://100.64.0.1"))
                .isInstanceOf(RequestException.class);
        assertThatThrownBy(() -> validatorReturning(address("0.0.0.42")).validate("http://0.0.0.42"))
                .isInstanceOf(RequestException.class);
    }

    @Test
    void uniqueLocalIpv6TargetsAreRejected() {
        assertThatThrownBy(() -> validatorReturning(address("fd12::1")).validate("http://[fd12::1]/v1"))
                .isInstanceOf(RequestException.class);
        assertThatThrownBy(() -> validatorReturning(address("fe80::1")).validate("http://[fe80::1]/v1"))
                .isInstanceOf(RequestException.class);
    }

    @Test
    void dnsRebindingWithMixedPublicAndPrivateAnswersIsRejected() {
        // The attacker's hostname answers with a public AND a private address:
        // every address must pass or the host is rejected.
        AgenticBaseUrlValidator validator = validatorReturning(address("93.184.216.34"), address("192.168.0.10"));
        assertThatThrownBy(() -> validator.validate("https://rebinding.example.com/v1"))
                .isInstanceOf(RequestException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void unresolvableHostIsRejected() {
        AgenticBaseUrlValidator validator = new AgenticBaseUrlValidator(host -> {
            throw new java.net.UnknownHostException(host);
        });
        assertThatThrownBy(() -> validator.validate("https://no-such-host.example.com"))
                .isInstanceOf(RequestException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void reactiveValidationMirrorsTheSyncVerdict() {
        AgenticBaseUrlValidator blocked = validatorReturning(address("127.0.0.1"));
        StepVerifier.create(blocked.validateReactive("http://localhost:11434/v1"))
                .expectError(RequestException.class)
                .verify();

        AgenticBaseUrlValidator allowed = validatorReturning(address("93.184.216.34"));
        StepVerifier.create(allowed.validateReactive("https://api.example.com")).verifyComplete();
    }

    @Test
    void verdictIsCachedForTheResolutionTtl() {
        // A host validated as public stays accepted even if the resolver flips
        // to a private answer right after: the verdict is cached, so a probe
        // cannot be smuggled through a re-resolution race.
        java.util.concurrent.atomic.AtomicReference<InetAddress> answer =
                new java.util.concurrent.atomic.AtomicReference<>(address("93.184.216.34"));
        AgenticBaseUrlValidator validator = new AgenticBaseUrlValidator(host -> new InetAddress[] {answer.get()});
        assertThatCode(() -> validator.validate("https://flip.example.com")).doesNotThrowAnyException();
        answer.set(address("192.168.0.10"));
        assertThatCode(() -> validator.validate("https://flip.example.com")).doesNotThrowAnyException();
    }
}
