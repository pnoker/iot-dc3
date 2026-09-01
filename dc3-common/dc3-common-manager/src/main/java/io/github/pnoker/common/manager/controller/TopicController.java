package io.github.pnoker.common.manager.controller;

import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.manager.entity.query.TopicOffsetQuery;
import io.github.pnoker.common.manager.entity.vo.TopicVO;
import io.github.pnoker.common.manager.service.ReactiveTopicService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Reactive topic projection endpoint. */
@Tag(name = "topic", description = "Message topic bindings")
@RestController
@RequestMapping(ManagerConstant.TOPIC_URL_PREFIX)
@RequiredArgsConstructor
public class TopicController implements BaseController {
    private final ReactiveTopicService topicService;

    @PreAuthorize("@perm.can('topic', 'list')")
    @Operation(summary = "List Topics", description = "List tenant-scoped topic bindings with offset pagination.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "false")
            }))
    @PostMapping("/list")
    public Mono<OffsetPage<TopicVO>> list(@RequestBody(required = false) TopicRequest request) {
        return getTenantId().flatMap(tenantId -> {
            TopicRequest value = request == null ? new TopicRequest(null, null, 0, 50) : request;
            return topicService.list(new TopicOffsetQuery(tenantId, value.topic(), value.deviceName(),
                    value.offset(), value.limit()));
        });
    }

    public record TopicRequest(
            @Schema(description = "Topic filter") String topic,
            @Schema(description = "Device name filter") String deviceName,
            @Schema(description = "Zero-based offset") long offset,
            @Schema(description = "Page size, 1..200") int limit) {
    }
}
