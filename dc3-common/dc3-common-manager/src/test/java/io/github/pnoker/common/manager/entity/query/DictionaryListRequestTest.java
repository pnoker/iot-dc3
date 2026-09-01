package io.github.pnoker.common.manager.entity.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DictionaryListRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void appliesDefaultsWhenPagingFieldsAreMissingFromJson() throws Exception {
        DictionaryListRequest request = objectMapper.readValue("{}", DictionaryListRequest.class);

        assertThat(request.offset()).isZero();
        assertThat(request.limit()).isEqualTo(50);
        assertThat(request.sort()).isEmpty();
    }

    @Test
    void rejectsUnsupportedSortField() {
        assertThatThrownBy(() -> new DictionaryListRequest(
                0L,
                50,
                List.of(new SortSpec("tenantId", SortSpec.Direction.ASC)),
                null,
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unsupported dictionary sort field");
    }
}
