package io.github.pnoker.common.auth.entity.builder;

import io.github.pnoker.common.auth.entity.bo.ApiBO;
import io.github.pnoker.common.auth.entity.model.ApiDO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiBuilderTest {

    @Test
    void afterProcessAllowsMissingApiTypeDuringEarlyValidation() {
        ApiBO source = new ApiBO();
        source.setApiName("GetDevice");
        ApiDO target = new ApiDO();

        new ApiBuilderImpl().afterProcess(source, target);

        assertThat(target.getApiTypeFlag()).isNull();
    }
}
