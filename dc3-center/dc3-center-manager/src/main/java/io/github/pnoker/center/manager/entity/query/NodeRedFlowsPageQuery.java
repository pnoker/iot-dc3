package io.github.pnoker.center.manager.entity.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.model.NodeRedFlows;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class NodeRedFlowsPageQuery extends NodeRedFlows {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Pages page;
}
