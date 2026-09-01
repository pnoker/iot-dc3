package io.github.pnoker.common.manager.biz.impl;

import io.github.pnoker.common.entity.base.BaseBO;
import io.github.pnoker.common.entity.option.DictionaryOption;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.manager.biz.DictionaryForManagerService;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.entity.query.DictionaryListRequest;
import io.github.pnoker.common.manager.repository.DeviceFilter;
import io.github.pnoker.common.manager.repository.DriverFilter;
import io.github.pnoker.common.manager.repository.PointFilter;
import io.github.pnoker.common.manager.repository.ProfileFilter;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.common.manager.service.ReactivePointService;
import io.github.pnoker.common.manager.service.ReactiveProfileService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

/** Default reactive option query service for manager entities. */
@Service
@RequiredArgsConstructor
public class DictionaryForManagerServiceImpl implements DictionaryForManagerService {

    private final ReactiveDriverService driverService;

    private final ReactiveProfileService profileService;

    private final ReactiveDeviceService deviceService;

    private final ReactivePointService pointService;

    @Override
    public Mono<OffsetPage<DictionaryOption>> listDriverOptions(Long tenantId, DictionaryListRequest request) {
        return driverService.list(new DriverFilter(
                        tenantId,
                        request.label(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        request.offset(),
                        request.limit(),
                        translateSort(request.sort(), "driverName")))
                .map(page -> toOptions(page, DriverBO::getDriverName));
    }

    @Override
    public Mono<OffsetPage<DictionaryOption>> listProfileOptions(Long tenantId, DictionaryListRequest request) {
        return profileService.list(new ProfileFilter(
                        tenantId,
                        request.label(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        request.offset(),
                        request.limit(),
                        translateSort(request.sort(), "profileName")))
                .map(page -> toOptions(page, ProfileBO::getProfileName));
    }

    @Override
    public Mono<OffsetPage<DictionaryOption>> listProfilePointOptions(Long tenantId, DictionaryListRequest request) {
        return requireParent(request)
                .then(Mono.defer(() -> pointService.list(new PointFilter(
                        tenantId,
                        request.label(),
                        null,
                        null,
                        null,
                        request.parentId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        request.offset(),
                        request.limit(),
                        translateSort(request.sort(), "pointName")))))
                .map(page -> toOptions(page, PointBO::getPointName));
    }

    @Override
    public Mono<OffsetPage<DictionaryOption>> listDevicePointOptions(Long tenantId, DictionaryListRequest request) {
        return requireParent(request)
                .then(Mono.defer(() -> pointService.list(new PointFilter(
                        tenantId,
                        request.label(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        request.parentId(),
                        request.offset(),
                        request.limit(),
                        translateSort(request.sort(), "pointName")))))
                .map(page -> toOptions(page, PointBO::getPointName));
    }

    @Override
    public Mono<OffsetPage<DictionaryOption>> listDeviceOptions(Long tenantId, DictionaryListRequest request) {
        return deviceService.list(new DeviceFilter(
                        tenantId,
                        request.label(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        request.offset(),
                        request.limit(),
                        translateSort(request.sort(), "deviceName")))
                .map(page -> toOptions(page, DeviceBO::getDeviceName));
    }

    @Override
    public Mono<OffsetPage<DictionaryOption>> listDriverDeviceOptions(Long tenantId, DictionaryListRequest request) {
        return requireParent(request)
                .then(Mono.defer(() -> deviceService.list(new DeviceFilter(
                        tenantId,
                        request.label(),
                        null,
                        request.parentId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        request.offset(),
                        request.limit(),
                        translateSort(request.sort(), "deviceName")))))
                .map(page -> toOptions(page, DeviceBO::getDeviceName));
    }

    private Mono<Void> requireParent(DictionaryListRequest request) {
        if (request.parentId() == null || request.parentId() <= 0) {
            return Mono.error(new RequestException("Parent ID is required"));
        }
        return Mono.empty();
    }

    private List<SortSpec> translateSort(List<SortSpec> sort, String labelField) {
        return sort.stream()
                .map(spec -> new SortSpec("label".equals(spec.field()) ? labelField : "id", spec.direction()))
                .toList();
    }

    private <T extends BaseBO> OffsetPage<DictionaryOption> toOptions(
            OffsetPage<T> page,
            Function<T, String> labelExtractor) {
        List<DictionaryOption> options = page.items().stream()
                .map(value -> DictionaryOption.leaf(labelExtractor.apply(value), value.getId().toString()))
                .toList();
        return OffsetPage.of(options, page.offset(), page.limit(), page.total());
    }

}
