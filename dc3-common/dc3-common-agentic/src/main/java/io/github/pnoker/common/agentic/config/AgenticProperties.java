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
     * Soft token budget for the assembled prompt context (system prompt + final user
     * message + replayed memory turns). When the estimate exceeds the budget the
     * oldest memory turns are dropped until it fits. Zero disables the budget.
     * Note: tool schemas and tool results are not yet counted — they are bounded
     * separately by the tool-result truncation cap.
     */
    @Min(value = 0, message = "Context token budget must not be negative")
    private int contextTokenBudget = 0;

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
     * Retry attempts the transport clients (openai-java / anthropic-java OkHttp clients)
     * perform for retryable HTTP failures — connection errors, 408, 409, 429 and 5xx —
     * with exponential backoff before surfacing the error to the stream. The SDK
     * default of 2 is too shallow under sustained provider rate limiting.
     */
    @Min(value = 0, message = "Transport max retries must not be negative")
    private int transportMaxRetries = 4;

    /**
     * Upper bound of concurrently in-flight model requests per provider. Requests
     * beyond the limit queue on the bounded-elastic scheduler instead of racing the
     * provider's rate limit; throttle at the source instead of retrying into it.
     */
    @Min(value = 1, message = "Transport max concurrent requests must be greater than 0")
    private int transportMaxConcurrentRequests = 4;

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

    // ===== Agent loop policy =====

    /**
     * Maximum agent loop rounds (model call + tool executions) per chat turn.
     * When the loop would exceed this bound the final round is replayed without
     * tools so the model is forced to produce a textual answer instead of the
     * turn failing hard, unless {@link #loopForceFinalRound} is disabled.
     */
    @Min(value = 1, message = "Loop max rounds must be greater than 0")
    private int loopMaxRounds = 8;

    /**
     * Whether the last permitted loop round is replayed without tools to force a
     * final textual answer. When false, exceeding {@link #loopMaxRounds} fails
     * the turn (legacy behavior).
     */
    private boolean loopForceFinalRound = true;

    // ===== Streaming backpressure policy =====

    /**
     * Frame budget the stream may buffer ahead of downstream demand before the
     * provider side is considered stalled. A slow SSE consumer is tolerated up
     * to this many buffered frames; the turn only fails once the buffer is full
     * AND demand stays absent for {@link #streamIdleTimeoutSeconds}.
     */
    @Min(value = 1, message = "Stream backpressure buffer must be greater than 0")
    private int streamBackpressureBuffer = 256;

    /**
     * Grace period in seconds during which a stream may sit with a full
     * backpressure buffer and zero downstream demand before failing the turn.
     * Shields long generations from transient consumer stalls (tab background,
     * slow proxies) without unbounded memory growth.
     */
    @Min(value = 1, message = "Stream idle timeout must be greater than 0 seconds")
    private int streamIdleTimeoutSeconds = 60;

    /**
     * Interval in seconds between SSE comment heartbeats emitted while a stream
     * is open, keeping intermediaries from closing idle connections and making
     * long tool rounds visible to consumers. Zero disables heartbeats.
     */
    @Min(value = 0, message = "Stream heartbeat must not be negative")
    private int streamHeartbeatSeconds = 15;

    // ===== Provider key encryption =====

    /**
     * Base64-encoded 256-bit master key used for AES-GCM envelope encryption of
     * provider API keys at rest. When blank, a stable key is derived from the
     * platform security key so local development keeps working; production must
     * set an explicit key (environment variable DC3_AGENTIC_MASTER_KEY).
     */
    private String securityMasterKey = "";

    // ===== Transport hardening =====

    /**
     * Maximum seconds a chat request may wait for an in-flight slot before
     * failing with an UNAVAILABLE problem instead of queueing forever.
     */
    @Min(value = 1, message = "Transport acquire timeout must be greater than 0 seconds")
    private int transportAcquireTimeoutSeconds = 30;

    /**
     * Upper bound of concurrently in-flight model requests across ALL providers
     * for paths that have no provider-specific semaphore (fallback model,
     * uncached model configs). Protects the process when per-provider limits
     * cannot apply.
     */
    @Min(value = 1, message = "Transport global max concurrent requests must be greater than 0")
    private int transportGlobalMaxConcurrentRequests = 16;

    /**
     * Whether a tenant without a model configuration silently falls back to the
     * platform-wide provider. Disabled by default: the fallback bypasses
     * per-tenant accounting, so it must be an explicit deployment decision.
     */
    private boolean transportFallbackEnabled = false;

    /**
     * Per-tenant chat requests per second limit enforced at the chat entry
     * point. Zero disables the limiter.
     */
    @Min(value = 0, message = "Per-tenant QPS must not be negative")
    private int transportPerTenantQps = 0;

    /**
     * Time-to-live in seconds for the in-process provider/model/client caches.
     * Bound stale entries after out-of-band provider edits in multi-replica
     * deployments; eviction notices from the local node refresh immediately.
     */
    @Min(value = 1, message = "Transport cache TTL must be greater than 0 seconds")
    private int transportCacheTtlSeconds = 300;

    /**
     * Upper bound of cached connectivity check results. Draft probes (no
     * provider id) use a small fixed partition of this budget so they cannot
     * grow the cache unboundedly.
     */
    @Min(value = 1, message = "Check cache maximum size must be greater than 0")
    private int checkCacheMaximumSize = 256;

    // ===== Attachment hardening =====

    /**
     * Comma-separated whitelist of attachment file extensions accepted for
     * upload. Anything outside the list is rejected before any bytes are
     * written.
     */
    private String attachmentAllowedExtensions = "png,jpg,jpeg,gif,webp,csv,txt,json,md,pdf";

    /**
     * Maximum number of stored attachments per tenant. Uploads beyond the
     * quota are rejected.
     */
    @Min(value = 1, message = "Attachment tenant max count must be greater than 0")
    private int attachmentTenantMaxCount = 2000;

    /**
     * Maximum total stored attachment bytes per tenant.
     */
    @Min(value = 1, message = "Attachment tenant max bytes must be greater than 0")
    private long attachmentTenantMaxBytes = 1073741824L;

    /**
     * Maximum length of an attachment file name accepted into prompts and
     * storage metadata; longer names are truncated. Part of the untrusted
     * content delimiting policy.
     */
    @Min(value = 16, message = "Attachment name max length must be at least 16")
    private int attachmentNameMaxLength = 128;
}
