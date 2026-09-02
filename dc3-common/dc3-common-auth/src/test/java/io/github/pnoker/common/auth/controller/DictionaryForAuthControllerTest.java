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
package io.github.pnoker.common.auth.controller;

import static org.mockito.Mockito.when;

import io.github.pnoker.common.auth.biz.DictionaryForAuthService;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.entity.option.DictionaryOption;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class DictionaryForAuthControllerTest {

    @Mock
    private DictionaryForAuthService dictionaryService;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToController(new DictionaryForAuthController(dictionaryService))
                .build();
    }

    @Test
    void listsTenantOptionsAtCanonicalCollectionPath() {
        when(dictionaryService.listTenantOptions())
                .thenReturn(Mono.just(List.of(DictionaryOption.leaf("Tenant A", "1001"))));

        client.get()
                .uri(AuthConstant.DICTIONARY_URL_PREFIX + "/list_tenant")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[0].label")
                .isEqualTo("Tenant A")
                .jsonPath("$[0].value")
                .isEqualTo("1001")
                .jsonPath("$[0].disabled")
                .isEqualTo(false)
                .jsonPath("$[0].expand")
                .isEqualTo(false)
                .jsonPath("$[0].children")
                .isArray();
    }

    @Test
    void doesNotExposeLegacyTenantDictionaryPath() {
        client.get()
                .uri(AuthConstant.DICTIONARY_URL_PREFIX + "/tenant")
                .exchange()
                .expectStatus()
                .isNotFound();
    }
}
