package io.github.pnoker.db.r2dbc.core.page;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageContractTest {

    @Test
    void derivesHasNextFromOffsetPageBounds() {
        OffsetPage<String> page = OffsetPage.of(List.of("a", "b"), 0, 2, 3);

        assertEquals(List.of("a", "b"), page.items());
        assertTrue(page.hasNext());
    }

    @Test
    void rejectsInvalidPageRequest() {
        assertThrows(IllegalArgumentException.class, () -> new PageRequest(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PageRequest(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> new PageRequest(0, PageRequest.MAX_LIMIT + 1));
        assertThrows(IllegalArgumentException.class, () -> new PageRequest(Long.MAX_VALUE, 1));
    }

    @Test
    void rejectsInconsistentOffsetPageMetadata() {
        assertThrows(IllegalArgumentException.class,
                () -> new OffsetPage<>(List.of("a"), 0, 1, 2, false));
        assertThrows(IllegalArgumentException.class,
                () -> new OffsetPage<>(List.of("a", "b"), 0, 1, 2, true));
    }

    @Test
    void requiresCursorOnlyWhenThereIsAnotherPage() {
        assertThrows(IllegalArgumentException.class, () -> new CursorPage<>(List.of(), null, true));
        assertThrows(IllegalArgumentException.class, () -> new CursorPage<>(List.of(), "cursor", true));
        assertThrows(IllegalArgumentException.class, () -> new CursorPage<>(List.of(), "cursor", false));
    }

    @Test
    void rejectsSortFieldsOutsideRepositoryWhitelist() {
        SortWhitelist whitelist = new SortWhitelist(
                Set.of("create_time", "name"), new SortSpec("create_time", SortSpec.Direction.DESC));

        assertEquals(List.of(new SortSpec("create_time", SortSpec.Direction.DESC)), whitelist.validate(List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> whitelist.validate(List.of(new SortSpec("drop table", SortSpec.Direction.ASC))));
        assertThrows(IllegalArgumentException.class,
                () -> whitelist.validate(List.of(
                        new SortSpec("name", SortSpec.Direction.ASC),
                        new SortSpec("name", SortSpec.Direction.DESC))));
    }
}
