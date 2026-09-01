package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.model.NotifyHistoryDO;

/** Result of an idempotent notification-history insert. */
public record NotifyHistoryInsertResult(NotifyHistoryDO history, boolean inserted) {
}
