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

package io.github.pnoker.common.agentic.config;

import io.github.pnoker.common.agentic.dal.ModelConfigManager;
import io.github.pnoker.common.agentic.dal.ModelProviderManager;
import io.github.pnoker.common.agentic.entity.builder.ModelConfigBuilder;
import io.github.pnoker.common.agentic.entity.builder.ModelProviderBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-Mockito coverage of {@link ChatClientFactory}. Provider lookup paths build
 * {@code Wrappers.lambdaQuery()} chains that need the MyBatis-Plus entity cache and
 * are deferred to integration tests; tests here exercise the resolveModel
 * shortcut and the per-provider cache eviction contract.
 */
@ExtendWith(MockitoExtension.class)
class ChatClientFactoryTest {

    @Mock
    private ModelProviderManager modelProviderManager;

    @Mock
    private ModelConfigManager modelConfigManager;

    @Mock
    private ModelProviderBuilder modelProviderBuilder;

    @Mock
    private ModelConfigBuilder modelConfigBuilder;

    @Mock
    private ChatClient.Builder fallbackBuilder;

    @Mock
    private Advisor memoryAdvisor;

    private ChatClientFactory factory;

    @BeforeEach
    void setUp() {
        factory = new ChatClientFactory(modelProviderManager, modelConfigManager, modelProviderBuilder,
                modelConfigBuilder, fallbackBuilder, memoryAdvisor);
    }

    @Test
    void resolveModelReturnsTrimmedNonBlankInputDirectly() {
        assertThat(factory.resolveModel("  gpt-4o  ")).isEqualTo("gpt-4o");
    }

    @Test
    void evictRemovesCachedClientForGivenProviderId() throws Exception {
        @SuppressWarnings("unchecked")
        Map<Long, ChatClient> cache = (Map<Long, ChatClient>) cacheField().get(factory);
        ChatClient cached = org.mockito.Mockito.mock(ChatClient.class);
        cache.put(7L, cached);

        factory.evict(7L);

        assertThat(cache).doesNotContainKey(7L);
    }

    @Test
    void evictIsNoOpForUnknownProviderId() throws Exception {
        @SuppressWarnings("unchecked")
        Map<Long, ChatClient> cache = (Map<Long, ChatClient>) cacheField().get(factory);
        ChatClient cached = org.mockito.Mockito.mock(ChatClient.class);
        cache.put(7L, cached);

        factory.evict(999L);

        assertThat(cache).containsKey(7L);
    }

    @Test
    void evictDoesNotThrowOnEmptyCache() throws Exception {
        @SuppressWarnings("unchecked")
        Map<Long, ChatClient> cache = (Map<Long, ChatClient>) cacheField().get(factory);
        assertThat(cache).isInstanceOf(ConcurrentHashMap.class);
        // Eviction with no entry present is a no-op.
        factory.evict(1L);
        factory.evict(2L);
        assertThat(cache).isEmpty();
    }

    private static Field cacheField() throws NoSuchFieldException {
        Field field = ChatClientFactory.class.getDeclaredField("cache");
        field.setAccessible(true);
        return field;
    }
}
