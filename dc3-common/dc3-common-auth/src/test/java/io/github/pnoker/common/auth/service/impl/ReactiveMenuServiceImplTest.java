package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.MenuBO;
import io.github.pnoker.common.auth.entity.builder.MenuBuilder;
import io.github.pnoker.common.auth.entity.model.MenuDO;
import io.github.pnoker.common.auth.repository.MenuFilter;
import io.github.pnoker.common.auth.repository.ReactiveMenuStore;
import io.github.pnoker.common.auth.repository.ReactiveResourceStore;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveMenuServiceImplTest {

    @Mock ReactiveMenuStore store;
    @Mock ReactiveResourceStore resourceStore;
    @Mock MenuBuilder builder;

    @Test
    void listTreeSortsChildren() {
        MenuDO parent = row(1L, 0L, "Parent", (byte) 0);
        MenuDO childB = row(3L, 1L, "B", (byte) 2);
        MenuDO childA = row(2L, 1L, "A", (byte) 1);
        when(store.listTree(any(MenuFilter.class))).thenReturn(Flux.just(parent, childB, childA));
        when(builder.buildBOByDO(any(MenuDO.class))).thenAnswer(invocation -> {
            MenuDO source = invocation.getArgument(0);
            MenuBO result = new MenuBO();
            result.setId(source.getId()); result.setParentMenuId(source.getParentMenuId());
            result.setMenuName(source.getMenuName()); result.setMenuIndex(source.getMenuIndex().intValue());
            return result;
        });

        StepVerifier.create(service().listTree(new MenuFilter(null, null, null, null, null, new PageRequest(0, 1))))
                .assertNext(root -> assertThat(root.getChildren()).extracting(MenuBO::getMenuName)
                        .containsExactly("A", "B"))
                .verifyComplete();
    }

    @Test
    void addMapsUniqueViolation() {
        MenuBO menu = validMenu();
        when(store.existsDuplicate(menu)).thenReturn(Mono.just(false));
        when(store.insert(menu)).thenReturn(Mono.error(new org.springframework.dao.DuplicateKeyException("duplicate")));

        StepVerifier.create(service().add(menu)).expectError(DuplicateException.class).verify();
    }

    @Test
    void updateRejectsCycle() {
        MenuBO menu = validMenu(); menu.setId(2L); menu.setParentMenuId(5L);
        when(store.getById(5L)).thenReturn(Mono.just(row(5L, 0L, "Ancestor", (byte) 0)));
        when(store.isDescendant(anyLong(), anyLong())).thenReturn(Mono.just(true));

        StepVerifier.create(service().update(menu)).expectError(RequestException.class).verify();
        verify(store, never()).existsDuplicate(any(MenuBO.class));
        verify(store, never()).update(any(MenuBO.class));
    }

    private ReactiveMenuServiceImpl service() { return new ReactiveMenuServiceImpl(store, resourceStore, builder); }

    private MenuBO validMenu() {
        MenuBO menu = new MenuBO(); menu.setParentMenuId(0L); menu.setMenuName("Menu");
        menu.setMenuCode("menu:read"); return menu;
    }

    private MenuDO row(Long id, Long parentId, String name, Byte index) {
        MenuDO row = new MenuDO(); row.setId(id); row.setParentMenuId(parentId);
        row.setMenuName(name); row.setMenuIndex(index); return row;
    }
}
