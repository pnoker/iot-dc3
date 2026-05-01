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
import io.github.pnoker.common.auth.entity.model.ResourceDO;
import io.github.pnoker.common.auth.mapper.ResourceRegistryLockMapper;
import io.github.pnoker.common.entity.ext.ApiExt;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.ApiTypeFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ResourceScopeFlagEnum;
import io.github.pnoker.common.enums.ResourceTypeFlagEnum;
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @author pnoker
 * @version 2026.4.30
 * @since 2026.4.30
 */
@Slf4j
@Service
public class ResourceRegistrySyncServiceImpl implements ResourceRegistrySyncService {

    /**
     * dc3_resource.resource_code prefix that brands rows owned by the API registrar.
     * Other resource types should use their own prefixes (e.g. {@code menu:}).
     */
    private static final String RESOURCE_CODE_PREFIX = "api:";

    @Resource
    private ApiManager apiManager;

    @Resource
    private ResourceManager resourceManager;

    @Resource
    private ResourceRegistryLockMapper resourceRegistryLockMapper;

    private static String apiCodeOf(String serviceName, String method, String path) {
        return serviceName + ":" + method.toUpperCase() + ":" + path;
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
        if (type == null) {
            return ResourceScopeFlagEnum.LIST;
        }
        return switch (type) {
            case POST -> ResourceScopeFlagEnum.ADD;
            case DELETE -> ResourceScopeFlagEnum.DELETE;
            case PUT -> ResourceScopeFlagEnum.UPDATE;
            case GET -> ResourceScopeFlagEnum.LIST;
        };
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

        int inserted = 0;
        int updated = 0;
        int deletedCount = 0;
        int unchanged = 0;

        List<ApiDO> apisToInsert = new ArrayList<>();
        List<ResourceDO> resourcesToInsert = new ArrayList<>();
        List<ApiDO> apisToUpdate = new ArrayList<>();
        List<ResourceDO> resourcesToUpdate = new ArrayList<>();
        List<Long> apiIdsToDelete = new ArrayList<>();

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
        }

        if (!apisToInsert.isEmpty()) {
            apiManager.saveBatch(apisToInsert);
            for (ApiDO api : apisToInsert) {
                resourcesToInsert.add(buildResourceDO(api));
            }
            resourceManager.saveBatch(resourcesToInsert);
            inserted = apisToInsert.size();
        }

        if (!apisToUpdate.isEmpty()) {
            apiManager.updateBatchById(apisToUpdate);
            Map<Long, ResourceDO> resourceByEntityId = loadResourcesByEntityIds(
                    apisToUpdate.stream().map(ApiDO::getId).toList());
            List<ResourceDO> resourcesToBackfill = new ArrayList<>();
            for (ApiDO api : apisToUpdate) {
                ResourceDO resourceDO = resourceByEntityId.get(api.getId());
                if (Objects.isNull(resourceDO)) {
                    resourcesToBackfill.add(buildResourceDO(api));
                } else {
                    applyResourceUpdates(resourceDO, api);
                    resourcesToUpdate.add(resourceDO);
                }
            }
            if (!resourcesToUpdate.isEmpty()) {
                resourceManager.updateBatchById(resourcesToUpdate);
            }
            if (!resourcesToBackfill.isEmpty()) {
                resourceManager.saveBatch(resourcesToBackfill);
            }
            updated = apisToUpdate.size();
        }

        if (command.isDeleteMissing() && !existingByCode.isEmpty()) {
            for (ApiDO orphan : existingByCode.values()) {
                apiIdsToDelete.add(orphan.getId());
            }
            apiManager.removeByIds(apiIdsToDelete);
            List<Long> resourceIds = resourceManager.list(Wrappers.<ResourceDO>lambdaQuery()
                            .eq(ResourceDO::getResourceTypeFlag, ResourceTypeFlagEnum.API.getIndex())
                            .in(ResourceDO::getEntityId, apiIdsToDelete))
                    .stream().map(ResourceDO::getId).toList();
            if (!resourceIds.isEmpty()) {
                resourceManager.removeByIds(resourceIds);
            }
            deletedCount = apiIdsToDelete.size();
        }

        log.info("Resource registry sync [{}]: inserted={}, updated={}, deleted={}, unchanged={}",
                serviceName, inserted, updated, deletedCount, unchanged);

        return ResourceRegistrySyncResult.builder()
                .inserted(inserted)
                .updated(updated)
                .deleted(deletedCount)
                .unchanged(unchanged)
                .build();
    }

    private Map<String, ApiDO> loadExisting(String serviceName) {
        List<ApiDO> existing = apiManager.list(Wrappers.<ApiDO>lambdaQuery()
                .eq(ApiDO::getServiceName, serviceName));
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
        List<ResourceDO> rows = resourceManager.list(Wrappers.<ResourceDO>lambdaQuery()
                .eq(ResourceDO::getResourceTypeFlag, ResourceTypeFlagEnum.API.getIndex())
                .in(ResourceDO::getEntityId, entityIds));
        Map<Long, ResourceDO> map = new HashMap<>(rows.size());
        for (ResourceDO row : rows) {
            map.put(row.getEntityId(), row);
        }
        return map;
    }

    private Map<String, ResourceRegistryScannedApi> indexScanned(List<ResourceRegistryScannedApi> scanned, String serviceName) {
        Map<String, ResourceRegistryScannedApi> map = new HashMap<>(scanned.size());
        for (ResourceRegistryScannedApi spec : scanned) {
            String code = apiCodeOf(serviceName, spec.getMethod(), spec.getPath());
            map.put(code, spec);
        }
        return map;
    }

    private ApiDO buildApiDO(ResourceRegistryScannedApi spec, String apiCode, String serviceName) {
        ApiDO api = new ApiDO();
        api.setServiceName(serviceName);
        api.setApiTypeFlag(methodToTypeFlag(spec.getMethod()).getIndex());
        api.setApiName(spec.getApiName());
        api.setApiCode(apiCode);
        api.setApiExt(buildApiExt(spec));
        api.setEnableFlag(EnableFlagEnum.ENABLE.getIndex());
        api.setRemark(Objects.requireNonNullElse(spec.getRemark(), ""));
        return api;
    }

    private ResourceDO buildResourceDO(ApiDO api) {
        ResourceDO resource = new ResourceDO();
        resource.setParentResourceId(0L);
        resource.setResourceName(api.getApiName());
        resource.setResourceCode(RESOURCE_CODE_PREFIX + api.getApiCode());
        resource.setResourceTypeFlag(ResourceTypeFlagEnum.API.getIndex());
        resource.setResourceScopeFlag(methodToScopeFlag(api.getApiTypeFlag()).getIndex());
        resource.setEntityId(api.getId());
        resource.setResourceExt(new JsonExt());
        resource.setEnableFlag(EnableFlagEnum.ENABLE.getIndex());
        resource.setRemark(Objects.requireNonNullElse(api.getRemark(), ""));
        return resource;
    }

    private boolean needsUpdate(ApiDO existing, ResourceRegistryScannedApi spec, String apiCode, String serviceName) {
        byte expectedType = methodToTypeFlag(spec.getMethod()).getIndex();
        if (existing.getApiTypeFlag() == null || existing.getApiTypeFlag() != expectedType) {
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
        existing.setApiTypeFlag(methodToTypeFlag(spec.getMethod()).getIndex());
        existing.setApiName(spec.getApiName());
        existing.setApiExt(buildApiExt(spec));
        existing.setRemark(Objects.requireNonNullElse(spec.getRemark(), ""));
        existing.setOperateTime(null);
    }

    private void applyResourceUpdates(ResourceDO resource, ApiDO api) {
        resource.setResourceName(api.getApiName());
        resource.setResourceCode(RESOURCE_CODE_PREFIX + api.getApiCode());
        resource.setResourceScopeFlag(methodToScopeFlag(api.getApiTypeFlag()).getIndex());
        resource.setRemark(Objects.requireNonNullElse(api.getRemark(), ""));
        resource.setOperateTime(null);
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
        if (a == null || b == null) {
            return a == null && b == null;
        }
        return Objects.equals(a.getTitle(), b.getTitle())
                && Objects.equals(a.getUrl(), b.getUrl())
                && Objects.equals(a.getRemark(), b.getRemark());
    }
}
