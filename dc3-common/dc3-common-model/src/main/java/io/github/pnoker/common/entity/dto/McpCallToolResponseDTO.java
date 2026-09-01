package io.github.pnoker.common.entity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/** Atomic MCP authorization decision and resolved downstream invocation. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpCallToolResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String decision;
    private String confirmId;
    private String message;
    private String riskLevel;
    private McpToolResolveResponseDTO tool;
    private McpPrincipalContextDTO principal;
}
