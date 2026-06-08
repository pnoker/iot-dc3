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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.pnoker.common.auth.biz.ResourceRegistrySyncService;
import io.github.pnoker.common.auth.dal.ApiManager;
import io.github.pnoker.common.auth.dal.ResourceManager;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistryScannedApi;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistrySyncCommand;
import io.github.pnoker.common.auth.entity.bo.ResourceRegistrySyncResult;
import io.github.pnoker.common.auth.entity.model.ApiDO;
import io.github.pnoker.common.auth.entity.model.MenuDO;
import io.github.pnoker.common.auth.entity.model.ResourceDO;
import io.github.pnoker.common.auth.mapper.ResourceRegistryLockMapper;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.entity.ext.ApiExt;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.ApiTypeFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ResourceScopeFlagEnum;
import io.github.pnoker.common.enums.ResourceTypeFlagEnum;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.pnoker.common.constant.service.AuthConstant.API_GROUP_NODE_CODE_PREFIX;
import static io.github.pnoker.common.constant.service.AuthConstant.API_SERVICE_NODE_CODE_PREFIX;
import static io.github.pnoker.common.constant.service.AuthConstant.MENU_RESOURCE_CODE_PREFIX;

/**
 * Auth-side reconciler for endpoint resource registration. The registrar submits a full
 * inventory for one service, and this service makes the API table plus permission
 * resource tree converge on that inventory inside a transaction scoped to the service
 * name.
 *
 * @author pnoker
 * @version 2026.5.17
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceRegistrySyncServiceImpl implements ResourceRegistrySyncService {

    private final ApiManager apiManager;
    private final ResourceManager resourceManager;
    private final ResourceRegistryLockMapper resourceRegistryLockMapper;

    private static String apiCodeOf(String serviceName, String method, String path) {
        return serviceName + SymbolConstant.COLON + methodToTypeFlag(method).name() + SymbolConstant.COLON + path;
    }

    private static ApiTypeFlagEnum methodToTypeFlag(String method) {
        String m = Objects.requireNonNullElse(method, "").toUpperCase();
        return switch (m) {
            case "POST" -> ApiTypeFlagEnum.POST;
            case "DELETE" -> ApiTypeFlagEnum.DELETE;
            case "PUT" -> ApiTypeFlagEnum.PUT;
            case "GET" -> ApiTypeFlagEnum.GET;
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };
    }

    private static ResourceScopeFlagEnum methodToScopeFlag(Byte apiTypeFlag) {
        ApiTypeFlagEnum type = ApiTypeFlagEnum.ofIndex(apiTypeFlag);
        if (Objects.isNull(type)) {
            return ResourceScopeFlagEnum.LIST;
        }
        return switch (type) {
            case POST -> ResourceScopeFlagEnum.ADD;
            case DELETE -> ResourceScopeFlagEnum.DELETE;
            case PUT -> ResourceScopeFlagEnum.UPDATE;
            case GET -> ResourceScopeFlagEnum.GET;
        };
    }

    private static String buildResourceCode(String serviceName, String apiName) {
        return Objects.requireNonNullElse(serviceName, "") + SymbolConstant.COLON
                + Objects.requireNonNullElse(apiName, "");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceRegistrySyncResult sync(ResourceRegistrySyncCommand command) {
        if (Objects.isNull(command) || StringUtils.isBlank(command.getServiceName())) {
            throw new IllegalArgumentException("serviceName is required");
        }
        String serviceName = command.getServiceName();
        List<ResourceRegistryScannedApi> scanned = Objects.requireNonNullElse(command.getApis(), List.of());

        resourceRegistryLockMapper.advisoryLock(serviceName);

        Map<String, ApiDO> existingByCode = loadExisting(serviceName);
        Map<String, ResourceRegistryScannedApi> scannedByCode = indexScanned(scanned, serviceName);

        // Ensure the service + apiGroup virtual grouping nodes exist before writing
        // leaves, so the leaf rows can set parent_resource_id to the right ancestor.
        Set<String> targetGroups = new LinkedHashSet<>();
        for (ResourceRegistryScannedApi spec : scannedByCode.values()) {
            targetGroups.add(Objects.requireNonNullElse(spec.getApiGroup(), ""));
        }
        Long serviceNodeId = null;
        Map<String, Long> groupNodeIds = Map.of();
        if (!targetGroups.isEmpty()) {
            serviceNodeId = ensureServiceNode(serviceName);
            groupNodeIds = ensureGroupNodes(serviceName, serviceNodeId, targetGroups);
        }

        int inserted = 0;
        int updated = 0;
        int deletedCount = 0;
        int unchanged = 0;

        List<ApiDO> apisToInsert = new ArrayList<>();
        List<ApiDO> apisToUpdate = new ArrayList<>();
        List<Long> apiIdsToDelete = new ArrayList<>();
        Map<String, ApiDO> targetApisByCode = new LinkedHashMap<>(scannedByCode.size());

        for (Map.Entry<String, ResourceRegistryScannedApi> entry : scannedByCode.entrySet()) {
            String apiCode = entry.getKey();
            ResourceRegistryScannedApi spec = entry.getValue();
            ApiDO existing = existingByCode.remove(apiCode);
            if (Objects.isNull(existing)) {
                ApiDO newApi = buildApiDO(spec, apiCode, serviceName);
                apisToInsert.add(newApi);
                continue;
            }
            if (needsUpdate(existing, spec, apiCode, serviceName)) {
                applyApiUpdates(existing, spec, apiCode, serviceName);
                apisToUpdate.add(existing);
            } else {
                unchanged++;
            }
            targetApisByCode.put(apiCode, existing);
        }

        if (!apisToInsert.isEmpty()) {
            apiManager.saveBatch(apisToInsert);
            for (ApiDO api : apisToInsert) {
                targetApisByCode.put(api.getApiCode(), api);
            }
            inserted = apisToInsert.size();
        }

        if (!apisToUpdate.isEmpty()) {
            apiManager.updateBatchById(apisToUpdate);
            updated = apisToUpdate.size();
        }

        ResourceRepairResult resourceRepair = reconcileLeafResources(new ArrayList<>(targetApisByCode.values()),
                groupNodeIds);

        if (command.isDeleteMissing() && !existingByCode.isEmpty()) {
            for (ApiDO orphan : existingByCode.values()) {
                apiIdsToDelete.add(orphan.getId());
            }
            apiManager.removeByIds(apiIdsToDelete);
            List<Long> resourceIds = resourceManager
                    .list(Wrappers.<ResourceDO>lambdaQuery()
                            .eq(ResourceDO::getResourceTypeFlag, ResourceTypeFlagEnum.API.getIndex())
                            .in(ResourceDO::getEntityId, apiIdsToDelete))
                    .stream()
                    .map(ResourceDO::getId)
                    .toList();
            if (!resourceIds.isEmpty()) {
                resourceManager.removeByIds(resourceIds);
            }
            deletedCount = apiIdsToDelete.size();
        }

        // Sweep orphaned apiGroup / service virtual nodes that no longer cover any leaf.
        int removedGroupNodes = cleanupOrphanGroupingNodes(serviceName);

        log.info(
                "Resource registry sync [{}]: inserted={}, updated={}, deleted={}, unchanged={}, resourceInserted={}, resourceUpdated={}, prunedGroupingNodes={}",
                serviceName, inserted, updated, deletedCount, unchanged, resourceRepair.inserted(),
                resourceRepair.updated(), removedGroupNodes);

        return ResourceRegistrySyncResult.builder()
                .inserted(inserted)
                .updated(updated)
                .deleted(deletedCount)
                .unchanged(unchanged)
                .build();
    }

    private Map<String, ApiDO> loadExisting(String serviceName) {
        List<ApiDO> existing = apiManager.list(Wrappers.<ApiDO>lambdaQuery().eq(ApiDO::getServiceName, serviceName));
        Map<String, ApiDO> map = new HashMap<>(existing.size());
        for (ApiDO api : existing) {
            map.put(api.getApiCode(), api);
        }
        return map;
    }

    private Map<Long, ResourceDO> loadResourcesByEntityIds(List<Long> entityIds) {
        if (entityIds.isEmpty()) {
            return Map.of();
        }
        // Exclude grouping nodes (entity_id=0) so the caller only sees real leaves.
        List<ResourceDO> rows = resourceManager.list(Wrappers.<ResourceDO>lambdaQuery()
                .eq(ResourceDO::getResourceTypeFlag, ResourceTypeFlagEnum.API.getIndex())
                .in(ResourceDO::getEntityId, entityIds)
                .ne(ResourceDO::getEntityId, 0L));
        Map<Long, ResourceDO> map = new HashMap<>(rows.size());
        for (ResourceDO row : rows) {
            map.put(row.getEntityId(), row);
        }
        return map;
    }

    private Map<String, ResourceRegistryScannedApi> indexScanned(List<ResourceRegistryScannedApi> scanned,
                                                                 String serviceName) {
        Map<String, ResourceRegistryScannedApi> map = new LinkedHashMap<>(scanned.size());
        for (ResourceRegistryScannedApi spec : scanned) {
            validateScannedApi(spec);
            String code = apiCodeOf(serviceName, spec.getMethod(), spec.getPath());
            if (Objects.nonNull(map.putIfAbsent(code, spec))) {
                log.warn("Duplicate scanned API ignored: serviceName={}, apiCode={}", serviceName, code);
            }
        }
        return map;
    }

    private void validateScannedApi(ResourceRegistryScannedApi spec) {
        if (Objects.isNull(spec)) {
            throw new IllegalArgumentException("Scanned API must not be null");
        }
        if (StringUtils.isBlank(spec.getMethod())) {
            throw new IllegalArgumentException("Scanned API method is required");
        }
        methodToTypeFlag(spec.getMethod());
        if (StringUtils.isBlank(spec.getPath())) {
            throw new IllegalArgumentException("Scanned API path is required");
        }
    }

    private ApiDO buildApiDO(ResourceRegistryScannedApi spec, String apiCode, String serviceName) {
        ApiDO api = new ApiDO();
        api.setServiceName(serviceName);
        api.setApiTypeFlag(methodToTypeFlag(spec.getMethod()).getIndex());
        api.setApiName(spec.getApiName());
        api.setApiCode(apiCode);
        api.setApiGroup(Objects.requireNonNullElse(spec.getApiGroup(), ""));
        api.setApiExt(buildApiExt(spec));
        api.setEnableFlag(EnableFlagEnum.ENABLE.getIndex());
        api.setRemark(Objects.requireNonNullElse(spec.getRemark(), ""));
        return api;
    }

    private ResourceDO buildLeafResourceDO(ApiDO api, Long groupNodeId) {
        ResourceDO resource = new ResourceDO();
        resource.setParentResourceId(Objects.requireNonNullElse(groupNodeId, 0L));
        resource.setResourceName(api.getApiName());
        resource.setResourceCode(buildResourceCode(api.getServiceName(), api.getApiName()));
        resource.setServiceName(api.getServiceName());
        resource.setResourceTypeFlag(ResourceTypeFlagEnum.API.getIndex());
        resource.setResourceScopeFlag(methodToScopeFlag(api.getApiTypeFlag()).getIndex());
        resource.setEntityId(api.getId());
        resource.setResourceExt(new JsonExt());
        resource.setEnableFlag(EnableFlagEnum.ENABLE.getIndex());
        resource.setRemark(Objects.requireNonNullElse(api.getRemark(), ""));
        return resource;
    }

    private ResourceRepairResult reconcileLeafResources(List<ApiDO> apis, Map<String, Long> groupNodeIds) {
        if (apis.isEmpty()) {
            return new ResourceRepairResult(0, 0);
        }
        for (ApiDO api : apis) {
            if (Objects.isNull(api.getId())) {
                throw new IllegalStateException("API id is required before creating resource leaf: " + api.getApiCode());
            }
        }
        List<Long> entityIds = apis.stream().map(ApiDO::getId).toList();
        Map<Long, ResourceDO> resourceByEntityId = loadResourcesByEntityIds(entityIds);
        List<ResourceDO> resourcesToInsert = new ArrayList<>();
        List<ResourceDO> resourcesToUpdate = new ArrayList<>();
        for (ApiDO api : apis) {
            Long groupNodeId = groupNodeIds.get(Objects.requireNonNullElse(api.getApiGroup(), ""));
            ResourceDO resourceDO = resourceByEntityId.get(api.getId());
            if (Objects.isNull(resourceDO)) {
                resourcesToInsert.add(buildLeafResourceDO(api, groupNodeId));
                continue;
            }
            if (needsLeafResourceUpdate(resourceDO, api, groupNodeId)) {
                applyLeafResourceUpdates(resourceDO, api, groupNodeId);
                resourcesToUpdate.add(resourceDO);
            }
        }
        if (!resourcesToInsert.isEmpty()) {
            resourceManager.saveBatch(resourcesToInsert);
        }
        if (!resourcesToUpdate.isEmpty()) {
            resourceManager.updateBatchById(resourcesToUpdate);
        }
        return new ResourceRepairResult(resourcesToInsert.size(), resourcesToUpdate.size());
    }

    private boolean needsUpdate(ApiDO existing, ResourceRegistryScannedApi spec, String apiCode, String serviceName) {
        byte expectedType = methodToTypeFlag(spec.getMethod()).getIndex();
        if (Objects.isNull(existing.getApiTypeFlag()) || existing.getApiTypeFlag() != expectedType) {
            return true;
        }
        if (!Objects.equals(existing.getApiName(), spec.getApiName())) {
            return true;
        }
        if (!Objects.equals(existing.getServiceName(), serviceName)) {
            return true;
        }
        if (!Objects.equals(existing.getApiCode(), apiCode)) {
            return true;
        }
        String expectedGroup = Objects.requireNonNullElse(spec.getApiGroup(), "");
        if (!Objects.equals(Objects.requireNonNullElse(existing.getApiGroup(), ""), expectedGroup)) {
            return true;
        }
        ApiExt.Content expectedContent = buildContent(spec);
        ApiExt.Content currentContent = parseContent(existing.getApiExt());
        if (!equalsContent(currentContent, expectedContent)) {
            return true;
        }
        String expectedRemark = Objects.requireNonNullElse(spec.getRemark(), "");
        return !Objects.equals(Objects.requireNonNullElse(existing.getRemark(), ""), expectedRemark);
    }

    private void applyApiUpdates(ApiDO existing, ResourceRegistryScannedApi spec, String apiCode, String serviceName) {
        existing.setServiceName(serviceName);
        existing.setApiCode(apiCode);
        existing.setApiGroup(Objects.requireNonNullElse(spec.getApiGroup(), ""));
        existing.setApiTypeFlag(methodToTypeFlag(spec.getMethod()).getIndex());
        existing.setApiName(spec.getApiName());
        existing.setApiExt(buildApiExt(spec));
        existing.setRemark(Objects.requireNonNullElse(spec.getRemark(), ""));
        existing.setOperateTime(null);
    }

    private void applyLeafResourceUpdates(ResourceDO resource, ApiDO api, Long groupNodeId) {
        resource.setParentResourceId(Objects.requireNonNullElse(groupNodeId, 0L));
        resource.setResourceName(api.getApiName());
        resource.setResourceCode(buildResourceCode(api.getServiceName(), api.getApiName()));
        resource.setServiceName(api.getServiceName());
        resource.setResourceTypeFlag(ResourceTypeFlagEnum.API.getIndex());
        resource.setResourceScopeFlag(methodToScopeFlag(api.getApiTypeFlag()).getIndex());
        resource.setEntityId(api.getId());
        if (Objects.isNull(resource.getResourceExt())) {
            resource.setResourceExt(new JsonExt());
        }
        resource.setRemark(Objects.requireNonNullElse(api.getRemark(), ""));
        resource.setOperateTime(null);
    }

    private boolean needsLeafResourceUpdate(ResourceDO resource, ApiDO api, Long groupNodeId) {
        if (!Objects.equals(resource.getParentResourceId(), Objects.requireNonNullElse(groupNodeId, 0L))) {
            return true;
        }
        if (!Objects.equals(resource.getResourceName(), api.getApiName())) {
            return true;
        }
        if (!Objects.equals(resource.getResourceCode(), buildResourceCode(api.getServiceName(), api.getApiName()))) {
            return true;
        }
        if (!Objects.equals(resource.getServiceName(), api.getServiceName())) {
            return true;
        }
        if (!Objects.equals(resource.getResourceTypeFlag(), ResourceTypeFlagEnum.API.getIndex())) {
            return true;
        }
        if (!Objects.equals(resource.getResourceScopeFlag(), methodToScopeFlag(api.getApiTypeFlag()).getIndex())) {
            return true;
        }
        if (!Objects.equals(resource.getEntityId(), api.getId())) {
            return true;
        }
        if (Objects.isNull(resource.getResourceExt())) {
            return true;
        }
        String expectedRemark = Objects.requireNonNullElse(api.getRemark(), "");
        return !Objects.equals(Objects.requireNonNullElse(resource.getRemark(), ""), expectedRemark);
    }

    private Long ensureServiceNode(String serviceName) {
        String code = API_SERVICE_NODE_CODE_PREFIX + serviceName;
        ResourceDO existing = findByResourceCode(code);
        if (Objects.nonNull(existing)) {
            if (needsGroupingNodeUpdate(existing, 0L, serviceName, code, "Service grouping node (auto-registered)")) {
                applyGroupingNodeUpdates(existing, 0L, serviceName, code, "Service grouping node (auto-registered)");
                resourceManager.updateById(existing);
            }
            return existing.getId();
        }
        ResourceDO node = new ResourceDO();
        node.setParentResourceId(0L);
        node.setResourceName(serviceName);
        node.setResourceCode(code);
        node.setResourceTypeFlag(ResourceTypeFlagEnum.API.getIndex());
        node.setResourceScopeFlag(ResourceScopeFlagEnum.LIST.getIndex());
        node.setEntityId(0L);
        node.setResourceExt(new JsonExt());
        node.setEnableFlag(EnableFlagEnum.ENABLE.getIndex());
        node.setRemark("Service grouping node (auto-registered)");
        resourceManager.save(node);
        return node.getId();
    }

    private Map<String, Long> ensureGroupNodes(String serviceName, Long serviceNodeId, Set<String> targetGroups) {
        Map<String, Long> result = new HashMap<>(targetGroups.size());

        // Build code→group lookup for both the batch query and the result
        Map<String, String> codeToGroup = new HashMap<>(targetGroups.size());
        List<String> allCodes = new ArrayList<>(targetGroups.size());
        for (String group : targetGroups) {
            String code = API_GROUP_NODE_CODE_PREFIX + serviceName + SymbolConstant.COLON + group;
            codeToGroup.put(code, group);
            allCodes.add(code);
        }

        // Batch-load all existing group nodes
        Map<String, ResourceDO> existingByCode = new HashMap<>();
        if (!allCodes.isEmpty()) {
            List<ResourceDO> existingList = resourceManager.list(Wrappers.<ResourceDO>lambdaQuery()
                    .in(ResourceDO::getResourceCode, allCodes));
            for (ResourceDO node : existingList) {
                existingByCode.put(node.getResourceCode(), node);
            }
        }

        List<ResourceDO> toInsert = new ArrayList<>();
        List<ResourceDO> toUpdate = new ArrayList<>();

        for (String group : targetGroups) {
            String code = API_GROUP_NODE_CODE_PREFIX + serviceName + SymbolConstant.COLON + group;
            ResourceDO existing = existingByCode.get(code);
            if (Objects.isNull(existing)) {
                ResourceDO node = new ResourceDO();
                node.setParentResourceId(serviceNodeId);
                node.setResourceName(group.isEmpty() ? "(ungrouped)" : group);
                node.setResourceCode(code);
                node.setResourceTypeFlag(ResourceTypeFlagEnum.API.getIndex());
                node.setResourceScopeFlag(ResourceScopeFlagEnum.LIST.getIndex());
                node.setEntityId(0L);
                node.setResourceExt(new JsonExt());
                node.setEnableFlag(EnableFlagEnum.ENABLE.getIndex());
                node.setRemark("API grouping node (auto-registered)");
                toInsert.add(node);
            } else {
                String name = group.isEmpty() ? "(ungrouped)" : group;
                if (needsGroupingNodeUpdate(existing, serviceNodeId, name, code, "API grouping node (auto-registered)")) {
                    applyGroupingNodeUpdates(existing, serviceNodeId, name, code, "API grouping node (auto-registered)");
                    toUpdate.add(existing);
                }
            }
        }

        if (!toInsert.isEmpty()) {
            resourceManager.saveBatch(toInsert);
            for (ResourceDO node : toInsert) {
                result.put(codeToGroup.get(node.getResourceCode()), node.getId());
            }
        }
        if (!toUpdate.isEmpty()) {
            resourceManager.updateBatchById(toUpdate);
        }
        for (ResourceDO existing : existingByCode.values()) {
            result.put(codeToGroup.get(existing.getResourceCode()), existing.getId());
        }

        return result;
    }

    private boolean needsGroupingNodeUpdate(ResourceDO node, Long parentResourceId, String name, String code,
                                            String remark) {
        if (!Objects.equals(node.getParentResourceId(), parentResourceId)) {
            return true;
        }
        if (!Objects.equals(node.getResourceName(), name)) {
            return true;
        }
        if (!Objects.equals(node.getResourceCode(), code)) {
            return true;
        }
        if (!Objects.equals(node.getResourceTypeFlag(), ResourceTypeFlagEnum.API.getIndex())) {
            return true;
        }
        if (!Objects.equals(node.getResourceScopeFlag(), ResourceScopeFlagEnum.LIST.getIndex())) {
            return true;
        }
        if (!Objects.equals(node.getEntityId(), 0L)) {
            return true;
        }
        if (Objects.isNull(node.getResourceExt())) {
            return true;
        }
        if (!Objects.equals(node.getEnableFlag(), EnableFlagEnum.ENABLE.getIndex())) {
            return true;
        }
        return !Objects.equals(Objects.requireNonNullElse(node.getRemark(), ""), remark);
    }

    private void applyGroupingNodeUpdates(ResourceDO node, Long parentResourceId, String name, String code,
                                          String remark) {
        node.setParentResourceId(parentResourceId);
        node.setResourceName(name);
        node.setResourceCode(code);
        node.setResourceTypeFlag(ResourceTypeFlagEnum.API.getIndex());
        node.setResourceScopeFlag(ResourceScopeFlagEnum.LIST.getIndex());
        node.setEntityId(0L);
        if (Objects.isNull(node.getResourceExt())) {
            node.setResourceExt(new JsonExt());
        }
        node.setEnableFlag(EnableFlagEnum.ENABLE.getIndex());
        node.setRemark(remark);
        node.setOperateTime(null);
    }

    private ResourceDO findByResourceCode(String code) {
        return resourceManager
                .getOne(Wrappers.<ResourceDO>lambdaQuery().eq(ResourceDO::getResourceCode, code).last("LIMIT 1"));
    }

    /**
     * Soft-delete apiGroup nodes that no longer have any leaf, then the service node
     * itself if it ends up with no remaining group children. Bounded to the given
     * serviceName so concurrent sync of sibling services is unaffected.
     */
    private int cleanupOrphanGroupingNodes(String serviceName) {
        int removed = 0;
        List<ResourceDO> groupNodes = resourceManager.list(Wrappers.<ResourceDO>lambdaQuery()
                .likeRight(ResourceDO::getResourceCode, API_GROUP_NODE_CODE_PREFIX + serviceName + SymbolConstant.COLON)
                .eq(ResourceDO::getEntityId, 0L));
        if (!groupNodes.isEmpty()) {
            // Single query to find which parents have children, instead of per-node COUNT
            List<Long> parentIds = groupNodes.stream().map(ResourceDO::getId).toList();
            Set<Long> parentsWithChildren = resourceManager.list(Wrappers.<ResourceDO>lambdaQuery()
                            .select(ResourceDO::getParentResourceId)
                            .in(ResourceDO::getParentResourceId, parentIds))
                    .stream()
                    .map(ResourceDO::getParentResourceId)
                    .collect(Collectors.toSet());
            List<Long> idsToDrop = new ArrayList<>();
            for (ResourceDO node : groupNodes) {
                if (!parentsWithChildren.contains(node.getId())) {
                    idsToDrop.add(node.getId());
                }
            }
            if (!idsToDrop.isEmpty()) {
                resourceManager.removeByIds(idsToDrop);
                removed += idsToDrop.size();
            }
        }
        ResourceDO serviceNode = findByResourceCode(API_SERVICE_NODE_CODE_PREFIX + serviceName);
        if (Objects.nonNull(serviceNode)) {
            long children = resourceManager
                    .count(Wrappers.<ResourceDO>lambdaQuery().eq(ResourceDO::getParentResourceId, serviceNode.getId()));
            if (children == 0) {
                resourceManager.removeById(serviceNode.getId());
                removed++;
            }
        }
        return removed;
    }

    private JsonExt buildApiExt(ResourceRegistryScannedApi spec) {
        JsonExt ext = new JsonExt();
        ext.setVersion(1);
        ext.setContent(JsonUtil.toJsonString(buildContent(spec)));
        return ext;
    }

    private ApiExt.Content buildContent(ResourceRegistryScannedApi spec) {
        ApiExt.Content content = new ApiExt.Content();
        content.setTitle(spec.getTitle());
        content.setUrl(spec.getPath());
        content.setRemark(spec.getRemark());
        return content;
    }

    private ApiExt.Content parseContent(JsonExt ext) {
        if (Objects.isNull(ext) || StringUtils.isBlank(ext.getContent())) {
            return null;
        }
        return JsonUtil.parseObject(ext.getContent(), ApiExt.Content.class);
    }

    private boolean equalsContent(ApiExt.Content a, ApiExt.Content b) {
        if (Objects.isNull(a) || Objects.isNull(b)) {
            return Objects.isNull(a) && Objects.isNull(b);
        }
        return Objects.equals(a.getTitle(), b.getTitle()) && Objects.equals(a.getUrl(), b.getUrl())
                && Objects.equals(a.getRemark(), b.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncMenuResource(MenuDO menu) {
        if (Objects.isNull(menu) || Objects.isNull(menu.getId())) {
            return;
        }
        String code = MENU_RESOURCE_CODE_PREFIX + Objects.requireNonNullElse(menu.getMenuCode(), "");
        // Prefer lookup by entity_id so a menu_code rename still resolves the mirror.
        ResourceDO existing = resourceManager.getOne(Wrappers.<ResourceDO>lambdaQuery()
                .eq(ResourceDO::getResourceTypeFlag, ResourceTypeFlagEnum.MENU.getIndex())
                .eq(ResourceDO::getEntityId, menu.getId())
                .last("LIMIT 1"));
        Long parentResourceId = resolveMenuParentResourceId(menu.getParentMenuId());

        if (Objects.isNull(existing)) {
            ResourceDO mirror = new ResourceDO();
            mirror.setParentResourceId(parentResourceId);
            mirror.setResourceName(Objects.requireNonNullElse(menu.getMenuName(), ""));
            mirror.setResourceCode(code);
            mirror.setResourceTypeFlag(ResourceTypeFlagEnum.MENU.getIndex());
            mirror.setResourceScopeFlag(ResourceScopeFlagEnum.LIST.getIndex());
            mirror.setEntityId(menu.getId());
            mirror.setResourceExt(new JsonExt());
            mirror.setEnableFlag(Objects.requireNonNullElse(menu.getEnableFlag(), EnableFlagEnum.ENABLE.getIndex()));
            mirror.setRemark(Objects.requireNonNullElse(menu.getRemark(), ""));
            resourceManager.save(mirror);
            log.info("Menu resource mirror inserted: menuId={}, code={}", menu.getId(), code);
        } else {
            existing.setParentResourceId(parentResourceId);
            existing.setResourceName(Objects.requireNonNullElse(menu.getMenuName(), ""));
            existing.setResourceCode(code);
            existing.setEnableFlag(Objects.requireNonNullElse(menu.getEnableFlag(), EnableFlagEnum.ENABLE.getIndex()));
            existing.setRemark(Objects.requireNonNullElse(menu.getRemark(), ""));
            existing.setOperateTime(null);
            resourceManager.updateById(existing);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMenuResource(Long menuId) {
        if (Objects.isNull(menuId)) {
            return;
        }
        ResourceDO existing = resourceManager.getOne(Wrappers.<ResourceDO>lambdaQuery()
                .eq(ResourceDO::getResourceTypeFlag, ResourceTypeFlagEnum.MENU.getIndex())
                .eq(ResourceDO::getEntityId, menuId)
                .last("LIMIT 1"));
        if (Objects.nonNull(existing)) {
            resourceManager.removeById(existing.getId());
        }
    }

    private Long resolveMenuParentResourceId(Long parentMenuId) {
        if (Objects.isNull(parentMenuId) || parentMenuId == 0L) {
            return 0L;
        }
        ResourceDO parent = resourceManager.getOne(Wrappers.<ResourceDO>lambdaQuery()
                .eq(ResourceDO::getResourceTypeFlag, ResourceTypeFlagEnum.MENU.getIndex())
                .eq(ResourceDO::getEntityId, parentMenuId)
                .last("LIMIT 1"));
        return Objects.isNull(parent) ? 0L : parent.getId();
    }

    /**
     * Counters for resource-tree repairs performed while keeping the public sync result
     * focused on dc3_api row changes.
     */
    private record ResourceRepairResult(int inserted, int updated) {
    }

}
