package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.entity.builder.ResourceBuilder;
import io.github.pnoker.common.auth.entity.model.ResourceDO;
import io.github.pnoker.common.auth.repository.ReactiveResourceStore;
import io.github.pnoker.common.auth.repository.ResourceFilter;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
class ReactiveResourceServiceImplTest {

    @Mock ReactiveResourceStore store;
    @Mock ResourceBuilder builder;

    @Test
    void listTreeDoesNotTruncateAndSortsChildren() {
        ResourceDO parent = row(1L, 0L, "Parent");
        ResourceDO childB = row(3L, 1L, "B");
        ResourceDO childA = row(2L, 1L, "A");
        when(store.listTree(any(ResourceFilter.class))).thenReturn(Flux.just(parent, childB, childA));
        when(builder.buildBOByDO(any(ResourceDO.class))).thenAnswer(invocation -> {
            ResourceDO source = invocation.getArgument(0);
            ResourceBO result = new ResourceBO();
            result.setId(source.getId());
            result.setParentResourceId(source.getParentResourceId());
            result.setResourceName(source.getResourceName());
            return result;
        });

        StepVerifier.create(service().listTree(new ResourceFilter(null, null, List.of(), List.of(), null, null,
                        new PageRequest(0, 1))))
                .assertNext(root -> {
                    assertThat(root.getId()).isEqualTo(1L);
                    assertThat(root.getChildren()).extracting(ResourceBO::getResourceName)
                            .containsExactly("A", "B");
                })
                .verifyComplete();
    }

    @Test
    void addMapsDatabaseDuplicateToDomainError() {
        ResourceBO resource = validResource();
        when(store.existsDuplicate(resource)).thenReturn(Mono.just(false));
        when(store.insert(resource)).thenReturn(Mono.error(new org.springframework.dao.DuplicateKeyException("duplicate")));

        StepVerifier.create(service().add(resource))
                .expectError(DuplicateException.class)
                .verify();
    }

    @Test
    void updateRejectsCyclesBeforeWriting() {
        ResourceBO resource = validResource();
        resource.setId(2L);
        resource.setParentResourceId(5L);
        when(store.getById(5L)).thenReturn(Mono.just(row(5L, 0L, "Ancestor")));
        when(store.isDescendant(anyLong(), anyLong())).thenReturn(Mono.just(true));

        StepVerifier.create(service().update(resource))
                .expectError(RequestException.class)
                .verify();
        verify(store, never()).existsDuplicate(any(ResourceBO.class));
        verify(store, never()).update(any(ResourceBO.class));
    }

    @Test
    void deleteRejectsParentResource() {
        when(store.getById(7L)).thenReturn(Mono.just(row(7L, 0L, "Parent")));
        when(builder.buildBOByDO(any(ResourceDO.class))).thenReturn(new ResourceBO());
        when(store.hasChildren(7L)).thenReturn(Mono.just(true));

        StepVerifier.create(service().delete(7L, 1L, "admin"))
                .expectError(RequestException.class)
                .verify();
        verify(store, never()).delete(7L, 1L, "admin");
    }

    private ReactiveResourceServiceImpl service() {
        return new ReactiveResourceServiceImpl(store, builder);
    }

    private ResourceBO validResource() {
        ResourceBO resource = new ResourceBO();
        resource.setParentResourceId(0L);
        resource.setResourceName("Resource");
        resource.setResourceCode("resource:read");
        resource.setEntityId(1L);
        return resource;
    }

    private ResourceDO row(Long id, Long parentId, String name) {
        ResourceDO row = new ResourceDO();
        row.setId(id);
        row.setParentResourceId(parentId);
        row.setResourceName(name);
        return row;
    }
}
