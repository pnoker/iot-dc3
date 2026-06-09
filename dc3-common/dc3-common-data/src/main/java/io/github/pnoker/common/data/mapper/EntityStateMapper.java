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

package io.github.pnoker.common.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mapper for dc3_entity_state table.
 *
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
@Mapper
public interface EntityStateMapper extends BaseMapper<EntityStateDO> {

    /**
     * Atomically inserts or renews one entity state lease.
     * <p>
     * The returned row contains the state after renewal; {@code lastStateFlag}
     * is the previous persisted state for updates, or the supplied initial value
     * for inserts.
     *
     * @param id                   generated id used only when inserting
     * @param tenantId             tenant id
     * @param entityTypeFlag       entity type flag
     * @param entityId             driver/device id
     * @param parentEntityId       owning entity id, or 0 for drivers
     * @param stateFlag            current state flag
     * @param initialLastStateFlag initial previous state for inserted rows
     * @param expireTime           lease expiry time
     * @param timeoutSeconds       lease timeout in seconds
     * @param timeoutSourceFlag    timeout source flag
     * @param stateExtType         state extension type for inserted rows
     * @param stateDescription     structured description for state_ext content
     * @return inserted or updated state row
     */
    EntityStateDO upsertEntityState(@Param("id") Long id,
                                    @Param("tenantId") Long tenantId,
                                    @Param("entityTypeFlag") Byte entityTypeFlag,
                                    @Param("entityId") Long entityId,
                                    @Param("parentEntityId") Long parentEntityId,
                                    @Param("stateFlag") Byte stateFlag,
                                    @Param("initialLastStateFlag") Byte initialLastStateFlag,
                                    @Param("expireTime") java.time.LocalDateTime expireTime,
                                    @Param("timeoutSeconds") int timeoutSeconds,
                                    @Param("timeoutSourceFlag") Byte timeoutSourceFlag,
                                    @Param("stateExtType") String stateExtType,
                                    @Param("stateDescription") String stateDescription);

    /**
     * Atomically claims expired online device leases and marks them offline.
     * <p>
     * PostgreSQL {@code FOR UPDATE SKIP LOCKED} lets multiple Data Center
     * instances split expired rows without blocking or processing the same row.
     *
     * @param entityTypeFlag      device entity type flag
     * @param onlineFlag          online state flag
     * @param maintainFlag        maintain state flag
     * @param faultFlag           fault state flag
     * @param offlineFlag         offline state flag
     * @param batchSize           maximum rows to claim
     * @param offlineRenewSeconds renewal window for already-offline state rows
     * @return claimed rows after the offline update; {@code lastStateFlag}
     * contains the previous state
     */
    List<EntityStateDO> claimExpiredDevices(@Param("entityTypeFlag") byte entityTypeFlag,
                                            @Param("onlineFlag") byte onlineFlag,
                                            @Param("maintainFlag") byte maintainFlag,
                                            @Param("faultFlag") byte faultFlag,
                                            @Param("offlineFlag") byte offlineFlag,
                                            @Param("batchSize") int batchSize,
                                            @Param("offlineRenewSeconds") int offlineRenewSeconds);

}
