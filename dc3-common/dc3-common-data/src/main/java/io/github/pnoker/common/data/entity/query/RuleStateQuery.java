package io.github.pnoker.common.data.entity.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import io.github.pnoker.common.enums.RuleStatusEnum;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(description = "Rule State offset query parameters")
public class RuleStateQuery implements Serializable {

    @Schema(description = "Zero-based row offset", example = "0")
    private long offset;

    @Schema(description = "Maximum rows to return", example = "50")
    @Builder.Default
    private int limit = PageRequest.DEFAULT_LIMIT;

    @Schema(description = "Whitelisted sort fields")
    @Builder.Default
    private List<SortSpec> sort = List.of();

    @Schema(description = "Filter by rule identifier")
    private Long ruleId;
    @Schema(description = "Filter by alarm target type")
    private AlarmTargetTypeEnum alarmTargetTypeFlag;
    @Schema(description = "Filter by associated entity identifier")
    private Long entityId;
    @Schema(description = "Filter by rule state fingerprint")
    private String fingerprint;
    @Schema(description = "Filter by current rule state")
    private RuleStatusEnum entityStateFlag;
    @Schema(description = "Filter by associated alarm identifier")
    private Long alarmId;
}
