package io.github.pnoker.common.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/** Principal context returned by auth for downstream request signing. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpPrincipalContextDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long tenantId;
    private Long principalId;
    private String principalType;
    private String principalName;
    private String displayName;
    private String clientId;
    private Long connectionId;
}
