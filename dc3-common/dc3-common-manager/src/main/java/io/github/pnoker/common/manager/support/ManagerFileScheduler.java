package io.github.pnoker.common.manager.support;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public final class ManagerFileScheduler {

    private final ExecutorService executor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("dc3-manager-file-", 0).factory());
    private final Scheduler scheduler = Schedulers.fromExecutorService(executor, "dc3-manager-file");

    public <T> Mono<T> call(Callable<T> task) {
        return Mono.fromCallable(task).subscribeOn(scheduler);
    }

    @PreDestroy
    void close() {
        scheduler.dispose();
        executor.shutdown();
    }
}
