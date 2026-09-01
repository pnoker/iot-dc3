package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ProfileShareTypeEnum;
import io.github.pnoker.common.enums.ProfileTypeEnum;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.ConflictException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.repository.ProfileFilter;
import io.github.pnoker.common.manager.repository.ReactiveProfileStore;
import io.github.pnoker.common.manager.service.ReactiveProfileService;
import io.github.pnoker.common.utils.CodeUtil;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReactiveProfileServiceImpl implements ReactiveProfileService {
    private final ReactiveProfileStore profileStore;

    @Override public Mono<ProfileBO> add(ProfileBO value) {
        return Mono.defer(() -> {
            validate(value, false);
            value.setProfileCode(value.getProfileCode() == null || value.getProfileCode().isBlank() ? CodeUtil.getCode() : value.getProfileCode().trim());
            return profileStore.existsByName(value.getTenantId(), value.getProfileName(), null)
                    .flatMap(duplicate -> duplicate ? Mono.<ProfileBO>error(new DuplicateException("Profile has been duplicated")) : profileStore.insert(normalize(value, false)))
                    .onErrorMap(DataIntegrityViolationException.class, error -> new DuplicateException("Profile has been duplicated"))
                    ;
        });
    }
    @Override public Mono<ProfileBO> update(ProfileBO value) {
        return Mono.defer(() -> {
            validate(value, true);
            return profileStore.get(value.getTenantId(), value.getId()).switchIfEmpty(Mono.error(new NotFoundException("Profile does not exist")))
                    .flatMap(current -> profileStore.existsByName(value.getTenantId(), value.getProfileName(), value.getId())
                            .flatMap(duplicate -> duplicate ? Mono.<ProfileBO>error(new DuplicateException("Profile has been duplicated")) : profileStore.update(normalize(value, true), value.getVersion())))
                    .switchIfEmpty(Mono.error(new ConflictException("Profile version conflict")))
                    ;
        });
    }
    @Override public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) {
        if (tenantId == null || id == null) return Mono.error(new RequestException("Tenant ID and profile ID are required"));
        return profileStore.get(tenantId, id).switchIfEmpty(Mono.error(new NotFoundException("Profile does not exist")))
                .flatMap(profile -> profileStore.hasAssociations(tenantId, id).flatMap(associated -> associated ? Mono.<Boolean>error(new RequestException("Profile is still referenced")) : profileStore.delete(tenantId, id, expectedVersion, operatorId, operatorName)))
                .filter(Boolean.TRUE::equals).switchIfEmpty(Mono.error(new ConflictException("Profile version conflict")))
;
    }
    @Override public Mono<ProfileBO> getById(Long tenantId, Long id) {
        if (tenantId == null || id == null) return Mono.error(new RequestException("Tenant ID and profile ID are required"));
        return profileStore.get(tenantId, id).switchIfEmpty(Mono.error(new NotFoundException("Profile does not exist")));
    }
    @Override public Mono<ProfileBO> getByNameAndType(Long tenantId, String name, ProfileTypeEnum type) { return profileStore.getByNameAndType(tenantId, name, type); }
    @Override public Flux<ProfileBO> listByIds(Long tenantId, List<Long> ids) { return profileStore.listByIds(tenantId, ids); }
    @Override public Flux<ProfileBO> listByDeviceId(Long tenantId, Long deviceId) { return profileStore.listByDeviceId(tenantId, deviceId); }
    @Override public Mono<OffsetPage<ProfileBO>> list(ProfileFilter filter) { return profileStore.list(filter); }

    private void validate(ProfileBO value, boolean update) {
        if (value == null || value.getTenantId() == null || value.getTenantId() <= 0 || value.getProfileName() == null || value.getProfileName().isBlank()) throw new RequestException("Tenant ID and profile name are required");
        if (update && (value.getId() == null || value.getVersion() == null || value.getVersion() < 0)) throw new RequestException("Profile ID and version are required for update");
        value.setProfileName(value.getProfileName().trim());
    }
    private ProfileBO normalize(ProfileBO value, boolean update) {
        value.setProfileShareFlag(value.getProfileShareFlag() == null ? ProfileShareTypeEnum.TENANT : value.getProfileShareFlag());
        value.setProfileTypeFlag(value.getProfileTypeFlag() == null ? ProfileTypeEnum.USER : value.getProfileTypeFlag());
        value.setEnableFlag(value.getEnableFlag() == null ? EnableFlagEnum.ENABLE : value.getEnableFlag());
        value.setRemark(value.getRemark() == null ? "" : value.getRemark());
        if (!update && value.getVersion() == null) value.setVersion(0);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (!update && value.getCreateTime() == null) value.setCreateTime(now);
        value.setOperateTime(now);
        return value;
    }

}
