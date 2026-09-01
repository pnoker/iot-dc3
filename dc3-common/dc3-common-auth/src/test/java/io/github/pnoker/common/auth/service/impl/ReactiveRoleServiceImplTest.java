package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.entity.model.RoleDO;
import io.github.pnoker.common.auth.repository.ReactiveRoleStore;
import io.github.pnoker.common.auth.repository.RoleFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveRoleServiceImplTest {
 @Mock ReactiveRoleStore store; @Mock RoleBuilder builder;
 @Test void listMapsOffsetPage(){ RoleDO row=new RoleDO(); RoleBO bo=new RoleBO(); when(store.list(any(RoleFilter.class))).thenReturn(Mono.just(OffsetPage.of(List.of(row),10,5,11))); when(builder.buildBOByDO(row)).thenReturn(bo); StepVerifier.create(service().list(new RoleFilter(7L,null,null,null,new io.github.pnoker.db.r2dbc.core.page.PageRequest(10,5,List.of())))).assertNext(p->{assertThat(p.items()).containsExactly(bo);assertThat(p.offset()).isEqualTo(10);assertThat(p.total()).isEqualTo(11);}).verifyComplete(); }
 @Test void getMissingRoleFailsClosed(){when(store.getById(7L,9L)).thenReturn(Mono.empty());StepVerifier.create(service().getById(7L,9L)).expectErrorMessage("Role").verify();}
 private ReactiveRoleServiceImpl service(){return new ReactiveRoleServiceImpl(store,builder);}
}
