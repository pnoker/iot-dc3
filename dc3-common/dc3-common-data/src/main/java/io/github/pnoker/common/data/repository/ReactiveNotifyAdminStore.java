package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.model.MessageDO;
import io.github.pnoker.common.data.entity.model.NotifyChannelBindDO;
import io.github.pnoker.common.data.entity.model.NotifyChannelDO;
import io.github.pnoker.common.data.entity.model.NotifyDO;
import io.github.pnoker.common.enums.AlarmMessageLevelEnum;
import io.github.pnoker.common.enums.AutoConfirmFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import reactor.core.publisher.Mono;

/** Reactive persistence port for notification configuration administration. */
public interface ReactiveNotifyAdminStore {
    Mono<NotifyDO> getNotify(long tenantId, long id);
    Mono<OffsetPage<NotifyDO>> listNotify(long tenantId, String name, String code, AutoConfirmFlagEnum autoConfirm,
                                          Long interval, EnableFlagEnum enable, PageRequest page);
    Mono<NotifyDO> insertNotify(NotifyDO value);
    Mono<NotifyDO> updateNotify(NotifyDO value);
    Mono<Boolean> deleteNotify(long tenantId, long id);
    Mono<Boolean> hasNotifyBindings(long tenantId, long notifyId);
    Mono<Boolean> existsNotifyCode(long tenantId, String code, Long excludedId);

    Mono<MessageDO> getMessage(long tenantId, long id);
    Mono<OffsetPage<MessageDO>> listMessage(long tenantId, String name, String code, AlarmMessageLevelEnum level,
                                            EnableFlagEnum enable, PageRequest page);
    Mono<MessageDO> insertMessage(MessageDO value);
    Mono<MessageDO> updateMessage(MessageDO value);
    Mono<Boolean> deleteMessage(long tenantId, long id);
    Mono<Boolean> existsMessageCode(long tenantId, String code, Long excludedId);

    Mono<NotifyChannelDO> getChannel(long tenantId, long id);
    Mono<OffsetPage<NotifyChannelDO>> listChannel(long tenantId, String name, String code, NotifyChannelTypeEnum type,
                                                  EnableFlagEnum enable, PageRequest page);
    Mono<NotifyChannelDO> insertChannel(NotifyChannelDO value);
    Mono<NotifyChannelDO> updateChannel(NotifyChannelDO value);
    Mono<Boolean> deleteChannel(long tenantId, long id);
    Mono<Boolean> hasChannelBindings(long tenantId, long channelId);
    Mono<Boolean> existsChannelCode(long tenantId, String code, Long excludedId);

    Mono<NotifyChannelBindDO> getBind(long tenantId, long id);
    Mono<OffsetPage<NotifyChannelBindDO>> listBind(long tenantId, Long notifyId, Long channelId,
                                                   EnableFlagEnum enable, PageRequest page);
    Mono<NotifyChannelBindDO> insertBind(NotifyChannelBindDO value);
    Mono<NotifyChannelBindDO> updateBind(NotifyChannelBindDO value);
    Mono<Boolean> deleteBind(long tenantId, long id);
    Mono<Boolean> existsBind(long tenantId, long notifyId, long channelId, Long excludedId);
    Mono<Boolean> existsNotify(long tenantId, long id);
    Mono<Boolean> existsChannel(long tenantId, long id);
}
