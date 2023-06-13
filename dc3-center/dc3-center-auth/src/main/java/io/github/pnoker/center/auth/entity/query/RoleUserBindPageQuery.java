package io.github.pnoker.center.auth.entity.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.model.RoleUserBind;
import lombok.*;

/**
 * @author linys
 * @since 2023.04.02
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RoleUserBindPageQuery extends RoleUserBind {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Pages page;
}
