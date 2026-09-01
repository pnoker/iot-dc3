package io.github.pnoker.common.entity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * Atomic MCP call authorization request. The bearer token is the only source of
 * principal context; callers cannot supply tenant or principal ids.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpCallToolRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String token;
    private String toolName;
    private String argumentDigest;
    private String confirmId;
    private String idempotencyKey;
    private String clientName;
    private String clientVersion;
    private String remoteIp;
}
