package io.github.pnoker.common.data.entity.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import io.github.pnoker.common.enums.NotifyHistoryStatusEnum;
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
@Schema(description = "Notify History offset query parameters")
public class NotifyHistoryQuery implements Serializable {

    @Schema(description = "Zero-based row offset", example = "0")
    private long offset;
    @Schema(description = "Maximum rows to return", example = "50")
    private int limit = PageRequest.DEFAULT_LIMIT;
    @Schema(description = "Whitelisted notification history sort fields")
    private List<SortSpec> sort = List.of();
    @Schema(description = "Filter by alarm rule identifier")
    private Long ruleId;
    @Schema(description = "Filter by notification policy identifier")
    private Long notifyId;
    @Schema(description = "Filter by message template identifier")
    private Long messageId;
    @Schema(description = "Filter by delivery channel identifier")
    private Long channelId;
    @Schema(description = "Filter by associated alarm identifier")
    private Long alarmId;
    @Schema(description = "Filter by notification channel type")
    private NotifyChannelTypeEnum channelTypeFlag;
    @Schema(description = "Filter by notification target text")
    private String target;
    @Schema(description = "Filter by delivery status")
    private NotifyHistoryStatusEnum statusFlag;
}
