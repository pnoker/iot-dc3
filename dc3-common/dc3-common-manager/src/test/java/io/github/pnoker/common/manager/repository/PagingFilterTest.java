package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PagingFilterTest {

    @Test
    void bindingFilterPreservesCanonicalOffsetContract() {
        BindingFilter filter = new BindingFilter(
                9L,
                EntityTypeEnum.DEVICE,
                20L,
                30L,
                40,
                20,
                List.of(new SortSpec("createTime", SortSpec.Direction.DESC)));

        assertThat(filter.offset()).isEqualTo(40);
        assertThat(filter.limit()).isEqualTo(20);
        assertThat(filter.sort()).containsExactly(new SortSpec("createTime", SortSpec.Direction.DESC));
    }

    @Test
    void filtersRejectMissingTenantScope() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new BindingFilter(0L, null, null, null, 0, 20, List.of()));
        assertThatIllegalArgumentException().isThrownBy(() ->
                new LabelFilter(null, null, null, null, null, 0, 20, List.of()));
        assertThatIllegalArgumentException().isThrownBy(() ->
                new GroupFilter(-1L, null, null, null, null, null, 0, 20, List.of()));
    }

    @Test
    void filtersRejectUnindexedSortFields() {
        SortSpec unsupported = new SortSpec("tenantId", SortSpec.Direction.ASC);

        assertThatIllegalArgumentException().isThrownBy(() ->
                new BindingFilter(1L, null, null, null, 0, 20, List.of(unsupported)));
        assertThatIllegalArgumentException().isThrownBy(() ->
                new LabelFilter(1L, null, null, null, null, 0, 20, List.of(unsupported)));
        assertThatIllegalArgumentException().isThrownBy(() ->
                new GroupFilter(1L, null, null, null, null, null, 0, 20, List.of(unsupported)));
    }

}
