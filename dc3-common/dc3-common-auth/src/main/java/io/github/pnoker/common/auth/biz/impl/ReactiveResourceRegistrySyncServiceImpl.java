/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.pnoker.common.auth.biz.impl;

import static io.github.pnoker.common.constant.service.AuthConstant.API_GROUP_NODE_CODE_PREFIX;
import static io.github.pnoker.common.constant.service.AuthConstant.API_SERVICE_NODE_CODE_PREFIX;

import io.github.pnoker.common.auth.biz.ReactiveResourceRegistrySyncService;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistryScannedApi;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistrySyncCommand;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistrySyncResult;
import io.github.pnoker.common.auth.entity.model.ApiDO;
import io.github.pnoker.common.auth.entity.model.ResourceDO;
import io.github.pnoker.common.auth.repository.ReactiveResourceRegistryStore;
import io.github.pnoker.common.auth.security.PermissionCacheInvalidator;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.entity.ext.ApiExt;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.ApiTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ResourceScopeTypeEnum;
import io.github.pnoker.common.enums.ResourceTypeEnum;
import io.github.pnoker.common.utils.JsonUtil;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Default resource registry sync service implementation. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReactiveResourceRegistrySyncServiceImpl implements ReactiveResourceRegistrySyncService {

    private final ReactiveResourceRegistryStore store;
    private final TransactionalOperator transactionalOperator;
    private final PermissionCacheInvalidator permissionCacheInvalidator;

    @Override
    public Mono<ResourceRegistrySyncResult> sync(ResourceRegistrySyncCommand command) {
        return Mono.defer(() -> {
            validateCommand(command);
            String serviceName = command.getServiceName().trim();
            Map<String, ResourceRegistryScannedApi> scanned = indexScanned(command.getApis(), serviceName);
            return transactionalOperator
                    .transactional(store.acquireLock("global:" + serviceName)
                            .timeout(Duration.ofSeconds(10))
                            .then(store.listApis(serviceName).collectMap(ApiDO::getApiCode, api -> api))
                            .flatMap(existing -> syncLocked(command, serviceName, scanned, existing)))
                    .doOnSuccess(result -> permissionCacheInvalidator.invalidateAll());
        });
    }

    private Mono<ResourceRegistrySyncResult> syncLocked(
            ResourceRegistrySyncCommand command,
            String serviceName,
            Map<String, ResourceRegistryScannedApi> scanned,
            Map<String, ApiDO> existing) {
        Set<String> groups = new LinkedHashSet<>();
        scanned.values().forEach(spec -> groups.add(normalize(spec.getApiGroup())));
        Mono<Map<String, Long>> groupNodes = groups.isEmpty()
                ? Mono.just(Map.of())
                : ensureServiceNode(serviceName)
                        .flatMapMany(serviceNode -> ensureGroupNodes(serviceName, serviceNode, groups))
                        .collectMap(Map.Entry::getKey, Map.Entry::getValue);

        return groupNodes.flatMap(groupIds -> Flux.fromIterable(scanned.entrySet())
                .concatMap(entry -> reconcileApi(entry.getKey(), entry.getValue(), serviceName, existing))
                .collectList()
                .flatMap(apiResults -> {
                    List<ApiDO> target =
                            apiResults.stream().map(ReconcileApi::api).toList();
                    int inserted = (int)
                            apiResults.stream().filter(ReconcileApi::inserted).count();
                    int updated = (int)
                            apiResults.stream().filter(ReconcileApi::updated).count();
                    int unchanged = (int) apiResults.stream()
                            .filter(value -> !value.inserted() && !value.updated())
                            .count();
                    return reconcileLeaves(target, groupIds)
                            .then(deleteMissing(command.isDeleteMissing(), existing.values(), scanned.keySet()))
                            .flatMap(deleted -> cleanupGroupingNodes(serviceName)
                                    .thenReturn(ResourceRegistrySyncResult.builder()
                                            .inserted(inserted)
                                            .updated(updated)
                                            .deleted(deleted)
                                            .unchanged(unchanged)
                                            .build()));
                }));
    }

    private Mono<ReconcileApi> reconcileApi(
            String code, ResourceRegistryScannedApi spec, String serviceName, Map<String, ApiDO> existing) {
        ApiDO current = existing.remove(code);
        if (current == null) {
            return store.insertApi(buildApi(spec, code, serviceName)).map(api -> new ReconcileApi(api, true, false));
        }
        if (!needsApiUpdate(current, spec, code, serviceName)) {
            return Mono.just(new ReconcileApi(current, false, false));
        }
        applyApi(current, spec, code, serviceName);
        return store.updateApi(current)
                .switchIfEmpty(Mono.error(new IllegalStateException("API update affected no rows")))
                .map(api -> new ReconcileApi(api, false, true));
    }

    private Mono<Void> reconcileLeaves(List<ApiDO> apis, Map<String, Long> groupIds) {
        if (apis.isEmpty()) return Mono.empty();
        Map<String, ApiDO> permissionApis = new LinkedHashMap<>();
        apis.forEach(api -> permissionApis.putIfAbsent(permissionCode(api), api));
        String serviceName = apis.getFirst().getServiceName();
        return store.listApiResources(serviceName).collectList().flatMap(resources -> {
            Map<String, ResourceDO> leaves = new LinkedHashMap<>();
            resources.stream()
                    .filter(resource -> resource.getEntityId() != null && resource.getEntityId() != 0)
                    .forEach(resource -> leaves.put(resource.getResourceCode(), resource));
            Mono<Void> upsert = Flux.fromIterable(permissionApis.values())
                    .concatMap(api -> {
                        Long parent = groupIds.get(normalize(api.getApiGroup()));
                        ResourceDO resource = leaves.remove(permissionCode(api));
                        if (resource == null)
                            return store.insertResource(buildLeaf(api, parent)).then();
                        if (!needsLeafUpdate(resource, api, parent)) return Mono.empty();
                        applyLeaf(resource, api, parent);
                        return store.updateResource(resource).then();
                    })
                    .then();
            Mono<Void> deleteStale = Flux.fromIterable(leaves.values())
                    .concatMap(resource -> store.deleteResource(resource.getId(), 0L, "resource-registry"))
                    .then();
            return upsert.then(deleteStale);
        });
    }

    private Mono<Integer> deleteMissing(
            boolean deleteMissing, java.util.Collection<ApiDO> orphans, Set<String> scannedCodes) {
        if (!deleteMissing || orphans.isEmpty()) return Mono.just(0);
        List<ApiDO> toDelete = orphans.stream()
                .filter(api -> !scannedCodes.contains(api.getApiCode()))
                .toList();
        if (toDelete.isEmpty()) return Mono.just(0);
        return Flux.fromIterable(toDelete)
                .concatMap(api -> store.deleteApi(api.getId(), 0L, "resource-registry"))
                .then(Mono.just(toDelete.size()));
    }

    private Mono<Void> cleanupGroupingNodes(String serviceName) {
        String serviceCode = API_SERVICE_NODE_CODE_PREFIX + serviceName;
        return store.listApiResources(serviceName)
                .collectList()
                .flatMapMany(resources -> Flux.fromIterable(resources)
                        .filter(resource -> resource.getEntityId() != null
                                && resource.getEntityId() == 0L
                                && resource.getResourceCode() != null
                                && resource.getResourceCode().startsWith(API_GROUP_NODE_CODE_PREFIX))
                        .filterWhen(resource ->
                                store.countChildren(resource.getId()).map(count -> count == 0))
                        .concatMap(resource -> store.deleteResource(resource.getId(), 0L, "resource-registry"))
                        .then(store.getResourceByCode(serviceCode)
                                .filterWhen(resource ->
                                        store.countChildren(resource.getId()).map(count -> count == 0))
                                .flatMap(resource -> store.deleteResource(resource.getId(), 0L, "resource-registry")))
                        .then())
                .then();
    }

    private Mono<Long> ensureServiceNode(String serviceName) {
        String code = API_SERVICE_NODE_CODE_PREFIX + serviceName;
        return store.getResourceByCode(code)
                .flatMap(existing -> {
                    if (needsGroupingUpdate(existing, serviceName, 0L, serviceName, code)) {
                        applyGrouping(existing, serviceName, 0L, serviceName, code);
                        return store.updateResource(existing).map(ResourceDO::getId);
                    }
                    return Mono.just(existing.getId());
                })
                .switchIfEmpty(Mono.defer(() -> store.insertResource(groupingNode(
                                serviceName, 0L, serviceName, code, "Service grouping node (auto-registered)"))
                        .map(ResourceDO::getId)));
    }

    private Flux<Map.Entry<String, Long>> ensureGroupNodes(String serviceName, Long serviceNodeId, Set<String> groups) {
        return Flux.fromIterable(groups).concatMap(group -> {
            String code = API_GROUP_NODE_CODE_PREFIX + serviceName + SymbolConstant.COLON + group;
            String name = group.isEmpty() ? "(ungrouped)" : group;
            return store.getResourceByCode(code)
                    .flatMap(existing -> {
                        if (needsGroupingUpdate(existing, serviceName, serviceNodeId, name, code)) {
                            applyGrouping(existing, serviceName, serviceNodeId, name, code);
                            return store.updateResource(existing);
                        }
                        return Mono.just(existing);
                    })
                    .switchIfEmpty(Mono.defer(() -> store.insertResource(groupingNode(
                            serviceName, serviceNodeId, name, code, "API grouping node (auto-registered)"))))
                    .map(node -> Map.entry(group, node.getId()));
        });
    }

    private ResourceDO groupingNode(String serviceName, Long parent, String name, String code, String remark) {
        ResourceDO node = new ResourceDO();
        node.setParentResourceId(parent);
        node.setResourceName(name);
        node.setResourceCode(code);
        node.setServiceName(serviceName);
        node.setResourceTypeFlag(ResourceTypeEnum.API.getIndex());
        node.setResourceScopeFlag(ResourceScopeTypeEnum.LIST.getIndex());
        node.setEntityId(0L);
        node.setResourceExt(new JsonExt());
        node.setEnableFlag(EnableFlagEnum.ENABLE.getIndex());
        node.setRemark(remark);
        return node;
    }

    private ResourceDO buildLeaf(ApiDO api, Long parent) {
        ResourceDO resource = new ResourceDO();
        resource.setParentResourceId(parent == null ? 0L : parent);
        resource.setResourceName(api.getApiName());
        resource.setResourceCode(permissionCode(api));
        resource.setServiceName(api.getServiceName());
        resource.setResourceTypeFlag(ResourceTypeEnum.API.getIndex());
        resource.setResourceScopeFlag(scope(api).getIndex());
        resource.setEntityId(api.getId());
        resource.setResourceExt(new JsonExt());
        resource.setEnableFlag(EnableFlagEnum.ENABLE.getIndex());
        resource.setRemark(normalize(api.getRemark()));
        return resource;
    }

    private ApiDO buildApi(ResourceRegistryScannedApi spec, String code, String serviceName) {
        ApiDO api = new ApiDO();
        applyApi(api, spec, code, serviceName);
        return api;
    }

    private void applyApi(ApiDO api, ResourceRegistryScannedApi spec, String code, String serviceName) {
        api.setServiceName(serviceName);
        api.setApiTypeFlag(methodType(spec.getMethod()).getIndex());
        api.setApiName(spec.getApiName());
        api.setApiCode(code);
        api.setApiGroup(normalize(spec.getApiGroup()));
        api.setApiExt(apiExt(spec));
        api.setEnableFlag(EnableFlagEnum.ENABLE.getIndex());
        api.setRemark(normalize(spec.getRemark()));
    }

    private void applyLeaf(ResourceDO resource, ApiDO api, Long parent) {
        resource.setParentResourceId(parent == null ? 0L : parent);
        resource.setResourceName(api.getApiName());
        resource.setResourceCode(permissionCode(api));
        resource.setServiceName(api.getServiceName());
        resource.setResourceTypeFlag(ResourceTypeEnum.API.getIndex());
        resource.setResourceScopeFlag(scope(api).getIndex());
        resource.setEntityId(api.getId());
        if (resource.getResourceExt() == null) resource.setResourceExt(new JsonExt());
        resource.setEnableFlag(EnableFlagEnum.ENABLE.getIndex());
        resource.setRemark(normalize(api.getRemark()));
    }

    private boolean needsApiUpdate(ApiDO api, ResourceRegistryScannedApi spec, String code, String serviceName) {
        return api.getApiTypeFlag() == null
                || api.getApiTypeFlag() != methodType(spec.getMethod()).getIndex()
                || !Objects.equals(api.getApiName(), spec.getApiName())
                || !Objects.equals(api.getServiceName(), serviceName)
                || !Objects.equals(api.getApiCode(), code)
                || !Objects.equals(normalize(api.getApiGroup()), normalize(spec.getApiGroup()))
                || !Objects.equals(JsonUtil.toJsonString(api.getApiExt()), JsonUtil.toJsonString(apiExt(spec)))
                || !Objects.equals(normalize(api.getRemark()), normalize(spec.getRemark()));
    }

    private boolean needsLeafUpdate(ResourceDO resource, ApiDO api, Long parent) {
        return !Objects.equals(resource.getParentResourceId(), parent == null ? 0L : parent)
                || !Objects.equals(resource.getResourceName(), api.getApiName())
                || !Objects.equals(resource.getResourceCode(), permissionCode(api))
                || !Objects.equals(resource.getServiceName(), api.getServiceName())
                || !Objects.equals(resource.getResourceTypeFlag(), ResourceTypeEnum.API.getIndex())
                || !Objects.equals(resource.getResourceScopeFlag(), scope(api).getIndex())
                || !Objects.equals(resource.getEntityId(), api.getId())
                || resource.getResourceExt() == null
                || !Objects.equals(normalize(resource.getRemark()), normalize(api.getRemark()));
    }

    private boolean needsGroupingUpdate(ResourceDO node, String serviceName, Long parent, String name, String code) {
        return !Objects.equals(node.getParentResourceId(), parent)
                || !Objects.equals(node.getResourceName(), name)
                || !Objects.equals(node.getResourceCode(), code)
                || !Objects.equals(node.getServiceName(), serviceName)
                || !Objects.equals(node.getResourceTypeFlag(), ResourceTypeEnum.API.getIndex())
                || !Objects.equals(node.getResourceScopeFlag(), ResourceScopeTypeEnum.LIST.getIndex())
                || !Objects.equals(node.getEntityId(), 0L)
                || node.getResourceExt() == null
                || !Objects.equals(node.getEnableFlag(), EnableFlagEnum.ENABLE.getIndex());
    }

    private void applyGrouping(ResourceDO node, String serviceName, Long parent, String name, String code) {
        node.setParentResourceId(parent);
        node.setResourceName(name);
        node.setResourceCode(code);
        node.setResourceTypeFlag(ResourceTypeEnum.API.getIndex());
        node.setServiceName(serviceName);
        node.setResourceScopeFlag(ResourceScopeTypeEnum.LIST.getIndex());
        node.setEntityId(0L);
        if (node.getResourceExt() == null) node.setResourceExt(new JsonExt());
        node.setEnableFlag(EnableFlagEnum.ENABLE.getIndex());
    }

    private String permissionCode(ApiDO api) {
        return api.getServiceName() + SymbolConstant.COLON + api.getApiName();
    }

    private JsonExt apiExt(ResourceRegistryScannedApi spec) {
        JsonExt ext = new JsonExt();
        ext.setVersion(1);
        ext.setContent(JsonUtil.toJsonString(new ApiExt.Content(spec.getTitle(), spec.getPath(), spec.getRemark())));
        return ext;
    }

    private ResourceScopeTypeEnum scope(ApiDO api) {
        String name = api.getApiName();
        int index = name == null ? -1 : name.lastIndexOf(SymbolConstant.COLON);
        if (index >= 0) {
            ResourceScopeTypeEnum explicit = ResourceScopeTypeEnum.ofCode(name.substring(index + 1));
            if (explicit != null) return explicit;
        }
        ApiTypeEnum type = ApiTypeEnum.ofIndex(api.getApiTypeFlag());
        return type == ApiTypeEnum.POST
                ? ResourceScopeTypeEnum.ADD
                : type == ApiTypeEnum.DELETE
                        ? ResourceScopeTypeEnum.DELETE
                        : (type == ApiTypeEnum.PUT || type == ApiTypeEnum.PATCH)
                                ? ResourceScopeTypeEnum.UPDATE
                                : type == ApiTypeEnum.GET ? ResourceScopeTypeEnum.GET : ResourceScopeTypeEnum.LIST;
    }

    private Map<String, ResourceRegistryScannedApi> indexScanned(
            List<ResourceRegistryScannedApi> apis, String serviceName) {
        Map<String, ResourceRegistryScannedApi> result = new LinkedHashMap<>();
        List<ResourceRegistryScannedApi> input = apis == null ? List.of() : apis;
        for (ResourceRegistryScannedApi spec : input) {
            validateScanned(spec);
            result.putIfAbsent(
                    serviceName
                            + SymbolConstant.COLON
                            + methodType(spec.getMethod()).name()
                            + SymbolConstant.COLON
                            + spec.getPath(),
                    spec);
        }
        return result;
    }

    private void validateCommand(ResourceRegistrySyncCommand command) {
        if (command == null || StringUtils.isBlank(command.getServiceName()))
            throw new IllegalArgumentException("serviceName is required");
    }

    private void validateScanned(ResourceRegistryScannedApi spec) {
        if (spec == null || StringUtils.isBlank(spec.getMethod()))
            throw new IllegalArgumentException("Scanned API method is required");
        methodType(spec.getMethod());
        if (StringUtils.isBlank(spec.getPath())) throw new IllegalArgumentException("Scanned API path is required");
        if (StringUtils.isBlank(spec.getApiName())) throw new IllegalArgumentException("Scanned API name is required");
    }

    private ApiTypeEnum methodType(String method) {
        return switch (Objects.requireNonNullElse(method, "").toUpperCase()) {
            case "POST" -> ApiTypeEnum.POST;
            case "DELETE" -> ApiTypeEnum.DELETE;
            case "PUT" -> ApiTypeEnum.PUT;
            case "PATCH" -> ApiTypeEnum.PATCH;
            case "GET" -> ApiTypeEnum.GET;
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private record ReconcileApi(ApiDO api, boolean inserted, boolean updated) {}
}
