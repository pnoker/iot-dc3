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
package io.github.pnoker.common.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM native-image hints for the entity extension DTOs. Every
 * device/driver/point/rule/notify payload carries an ext field that Jackson
 * deserializes reflectively; Spring AOT bean processing does not cover these
 * plain DTOs, so the whole family is registered with constructor, field and
 * method access.
 *
 * @author pnoker
 */
public class ModelNativeRuntimeHints implements RuntimeHintsRegistrar {

    private static final String[] EXT_CLASSES = {
        "ApiExt",
        "BaseExt",
        "CommandAttributeExt",
        "CommandExt",
        "CommandParamExt",
        "DeviceExt",
        "DriverAttributeExt",
        "DriverExt",
        "EventAttributeExt",
        "EventExt",
        "EventParamExt",
        "JsonExt",
        "MenuExt",
        "MessageExt",
        "NotifyChannelBindExt",
        "NotifyChannelExt",
        "NotifyExt",
        "NotifyHistoryRequestExt",
        "NotifyHistoryResponseExt",
        "PointAttributeExt",
        "PointExt",
        "ProfileExt",
        "ResourceExt",
        "RoleExt",
        "RuleAlarmEventExt",
        "RuleExt",
        "RuleStateExt",
        "TenantExt",
        "UserIdentityExt",
        "UserSocialExt",
    };

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        for (String name : EXT_CLASSES) {
            hints.reflection().registerType(
                    org.springframework.aot.hint.TypeReference.of(
                            "io.github.pnoker.common.entity.ext." + name),
                    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.PUBLIC_FIELDS,
                    MemberCategory.DECLARED_FIELDS,
                    MemberCategory.INVOKE_PUBLIC_METHODS,
                    MemberCategory.INVOKE_DECLARED_METHODS);
        }
    }
}
