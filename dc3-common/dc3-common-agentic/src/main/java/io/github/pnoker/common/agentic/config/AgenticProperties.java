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

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the agentic module.
 *
 * <p>
 * Prefix: {@code dc3.agentic}
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "dc3.agentic")
public class AgenticProperties {

    /**
     * Whether to enable the agentic module.
     */
    private boolean enabled = true;

    /**
     * Maximum number of messages retained per conversation by the in-memory window
     * before older turns are evicted.
     */
    @Min(value = 1, message = "Memory max messages must be greater than 0")
    private int memoryMaxMessages = 50;

    /**
     * Whether to replay persisted conversation history back to the model provider on
     * each request. When false the assistant remains fully stateless across turns.
     */
    private boolean memoryEnabled = true;

    /**
     * Maximum number of historical messages (user + assistant turns combined) loaded
     * from {@code dc3_message} and replayed to the model per request. Independent of
     * {@link #memoryMaxMessages}, which controls the in-memory advisor window.
     */
    @Min(value = 1, message = "History window size must be greater than 0")
    private int historyWindowSize = 30;

    /**
     * Whether Spring AI should execute provider-native tool calls.
     */
    private boolean toolCallingEnabled = true;

    /**
     * Whether the Spring AI fallback model should be advertised and executed with
     * provider-native tool calls when no database model configuration exists.
     */
    private boolean fallbackToolCallingEnabled = true;

    /**
     * Whether the Spring AI fallback model should be advertised as vision-capable
     * when no database model configuration exists.
     */
    private boolean fallbackVisionEnabled = true;

    /**
     * Whether the Spring AI fallback model should be advertised as reasoning-capable
     * when no database model configuration exists.
     */
    private boolean fallbackReasoningEnabled = false;

    /**
     * Root directory used to persist uploaded agentic attachment files. Runtime files
     * are stored under tenant/user/conversation subfolders.
     */
    private String attachmentStoragePath = "dc3/data/upload/agentic/attachment";

    /**
     * Timeout budget in seconds for the L1 model-list connectivity probe. The
     * probe client never inherits the chat transport defaults: it builds a
     * one-shot OkHttp client with this timeout and zero retries.
     */
    @Min(value = 1, message = "L1 check timeout must be greater than 0 seconds")
    private int checkL1TimeoutSeconds = 5;

    /**
     * Timeout budget in seconds for the L2 minimal chat probe. Real completions
     * can be slow on cold providers, hence the wider budget than L1.
     */
    @Min(value = 1, message = "L2 check timeout must be greater than 0 seconds")
    private int checkL2TimeoutSeconds = 30;

    /**
     * Time-to-live in seconds for cached connectivity check results. Repeated
     * triggers inside the window return the cached result without touching the
     * upstream provider again.
     */
    @Min(value = 1, message = "Check result cache TTL must be greater than 0 seconds")
    private int checkCacheTtlSeconds = 30;

    /**
     * Maximum model ids returned by the L1 model-list probe. The probe reads a
     * single page and truncates; it never walks the full catalog.
     */
    @Min(value = 1, message = "Check max models must be greater than 0")
    private int checkMaxModels = 100;
}
