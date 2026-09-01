package io.github.pnoker.common.auth.support;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/** Bounded worker pool for CPU-heavy password and JWT operations. */
public final class ReactiveAuthScheduler {

    public static final Scheduler CRYPTO = Schedulers.newBoundedElastic(4, 10_000, "dc3-auth-crypto");

    private ReactiveAuthScheduler() {
    }
}
