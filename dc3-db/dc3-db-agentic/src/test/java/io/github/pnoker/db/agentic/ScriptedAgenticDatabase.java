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
package io.github.pnoker.db.agentic;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.mockito.ArgumentMatchers;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.FetchSpec;
import org.springframework.r2dbc.core.RowsFetchSpec;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * In-memory stand-in for the agentic PostgreSQL schema, purpose-built for the store unit tests.
 *
 * <p>Routes the stores' explicit SQL by statement shape, keeps mutable session, attachment, and
 * message rows, records every executed statement with its bind values, and models READ COMMITTED
 * row locking: {@code SELECT ... FOR UPDATE} serializes per conversation (FIFO waiters that
 * re-read row state once granted), a session-row mutation releases the lock, and a natural-key
 * insert colliding with an existing row raises {@link DataIntegrityViolationException}. Tests can
 * park one statement mid-flight with {@link #holdNext(String)} to force real interleavings.
 */
final class ScriptedAgenticDatabase implements DatabaseClient {

    private final Map<String, SessionRow> sessions = new LinkedHashMap<>();
    private final List<AttachmentRow> attachments = new ArrayList<>();
    private final List<MessageRow> messages = new ArrayList<>();
    private final List<ActionRow> actions = new ArrayList<>();
    private final Map<String, LockBox> locks = new LinkedHashMap<>();
    private final List<Execution> executions = new ArrayList<>();
    private final ArrayDeque<Hold> holds = new ArrayDeque<>();
    private final AtomicLong sessionIds = new AtomicLong(100);
    private final AtomicLong attachmentIds = new AtomicLong(200);
    private final AtomicLong messageIds = new AtomicLong(300);
    private final AtomicLong actionIds = new AtomicLong(400);
    private Runnable sessionInsertViolationScript;

    /** One recorded statement with its bind values and typed null binds. */
    record Execution(String sql, Map<String, Object> binds, Map<String, Class<?>> nulls) {

        boolean contains(String marker) {
            return sql.contains(marker);
        }
    }

    /** A mutable soft-deletable {@code dc3_session} row. */
    static final class SessionRow {
        Long id;
        String conversationId;
        String title;
        String sessionExtJson;
        Long tenantId;
        Long userId;
        Long creatorId;
        String creatorName;
        Object createTime;
        Long operatorId;
        String operatorName;
        Object operateTime;
        int deleted;
    }

    /** A mutable soft-deletable {@code dc3_attachment} row. */
    static final class AttachmentRow {
        Long id;
        String conversationId;
        String fileName;
        String contentType;
        Long size;
        String filePath;
        Long tenantId;
        Long userId;
        Long creatorId;
        String creatorName;
        Instant createTime;
        Long operatorId;
        String operatorName;
        Instant operateTime;
        int deleted;
    }

    /** A mutable soft-deletable {@code dc3_message} row. */
    static final class MessageRow {
        Long id;
        String conversationId;
        String role;
        String content;
        String model;
        Long messageIndex;
        Byte status;
        Long tenantId;
        Long userId;
        Instant createTime;
        int deleted;
    }

    /** A mutable soft-deletable {@code dc3_action} row. */
    static final class ActionRow {
        Long id;
        String actionId;
        String conversationId;
        String actionType;
        String title;
        String description;
        String payload;
        Byte status;
        Instant expireTime;
        Long tenantId;
        Long userId;
        Instant createTime;
        int deleted;
    }

    /** Park the next statement whose SQL contains the marker until {@link Hold#release()}. */
    Hold holdNext(String marker) {
        Hold hold = new Hold(marker);
        synchronized (holds) {
            holds.add(hold);
        }
        return hold;
    }

    /** Fail the next session insert with a unique-key violation, running the script first. */
    void scriptNextSessionInsertViolation(Runnable script) {
        sessionInsertViolationScript = script;
    }

    List<Execution> executions() {
        synchronized (executions) {
            return List.copyOf(executions);
        }
    }

    Execution firstMatching(String marker) {
        return executions().stream()
                .filter(execution -> execution.contains(marker))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no statement containing: " + marker));
    }

    int countMatching(String marker) {
        return (int) executions().stream()
                .filter(execution -> execution.contains(marker))
                .count();
    }

    int indexOfFirst(String marker) {
        List<Execution> all = executions();
        for (int index = 0; index < all.size(); index++) {
            if (all.get(index).contains(marker)) return index;
        }
        return -1;
    }

    void seedSession(
            long tenantId, long userId, String conversationId, String title, String sessionExtJson, boolean deleted) {
        SessionRow row = new SessionRow();
        row.id = sessionIds.incrementAndGet();
        row.tenantId = tenantId;
        row.userId = userId;
        row.conversationId = conversationId;
        row.title = title;
        row.sessionExtJson = sessionExtJson;
        row.creatorId = userId;
        row.creatorName = "seeder";
        row.operatorId = userId;
        row.operatorName = "seeder";
        row.createTime = Instant.parse("2026-01-01T00:00:00Z");
        row.operateTime = row.createTime;
        row.deleted = deleted ? 1 : 0;
        sessions.put(key(tenantId, userId, conversationId), row);
    }

    /** Seed a session whose decoded times carry a non-UTC offset, like a driver in a non-UTC session timezone. */
    void seedSessionWithOffsetTimes(long tenantId, long userId, String conversationId) {
        seedSession(tenantId, userId, conversationId, "Offset", "{}", false);
        SessionRow row = sessions.get(key(tenantId, userId, conversationId));
        row.createTime = java.time.OffsetDateTime.parse("2026-03-01T12:00:00+08:00");
        row.operateTime = java.time.OffsetDateTime.parse("2026-03-01T12:00:00+08:00");
    }

    void seedAttachment(long tenantId, long userId, String conversationId, String filePath, boolean deleted) {
        seedAttachment(tenantId, userId, conversationId, filePath, 1L, deleted);
    }

    void seedAttachment(
            long tenantId, long userId, String conversationId, String filePath, long size, boolean deleted) {
        AttachmentRow row = new AttachmentRow();
        row.id = attachmentIds.incrementAndGet();
        row.tenantId = tenantId;
        row.userId = userId;
        row.conversationId = conversationId;
        row.fileName = "seed.txt";
        row.contentType = "text/plain";
        row.size = size;
        row.filePath = filePath;
        row.creatorId = userId;
        row.createTime = Instant.parse("2026-01-01T00:00:00Z");
        row.deleted = deleted ? 1 : 0;
        attachments.add(row);
    }

    SessionRow session(long tenantId, long userId, String conversationId) {
        return sessions.get(key(tenantId, userId, conversationId));
    }

    List<AttachmentRow> attachments() {
        return attachments;
    }

    List<MessageRow> messages() {
        return messages;
    }

    /** A transactional operator mock that passes publishers through unchanged. */
    static TransactionalOperator passThroughOperator() {
        TransactionalOperator operator = org.mockito.Mockito.mock(TransactionalOperator.class);
        org.mockito.Mockito.when(operator.transactional(ArgumentMatchers.<Mono<Object>>any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.when(operator.transactional(ArgumentMatchers.<Flux<Object>>any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        return operator;
    }

    /** A page transaction mock that passes publishers through unchanged. */
    static io.github.pnoker.db.core.transaction.PageTransaction passThroughPageTransaction() {
        io.github.pnoker.db.core.transaction.PageTransaction transaction =
                org.mockito.Mockito.mock(io.github.pnoker.db.core.transaction.PageTransaction.class);
        org.mockito.Mockito.when(transaction.transactional(ArgumentMatchers.<Mono<Object>>any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        return transaction;
    }

    @Override
    public GenericExecuteSpec sql(String sql) {
        return new ScriptedSpec(sql);
    }

    @Override
    public GenericExecuteSpec sql(Supplier<String> sqlSupplier) {
        return new ScriptedSpec(sqlSupplier.get());
    }

    @Override
    public ConnectionFactory getConnectionFactory() {
        throw new UnsupportedOperationException("not used by the store unit tests");
    }

    @Override
    public <T> Mono<T> inConnection(java.util.function.Function<io.r2dbc.spi.Connection, Mono<T>> connectionFunction) {
        throw new UnsupportedOperationException("not used by the store unit tests");
    }

    @Override
    public <T> Flux<T> inConnectionMany(
            java.util.function.Function<io.r2dbc.spi.Connection, Flux<T>> connectionFunction) {
        throw new UnsupportedOperationException("not used by the store unit tests");
    }

    /** Parked-statement handle; the routed latch proves the statement reached the database. */
    static final class Hold {
        private final String marker;
        private final CompletableFuture<Void> gate = new CompletableFuture<>();
        private final CountDownLatch routed = new CountDownLatch(1);

        private Hold(String marker) {
            this.marker = marker;
        }

        void awaitRouted() throws InterruptedException {
            if (!routed.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
                throw new AssertionError("held statement never routed: " + marker);
            }
        }

        void release() {
            gate.complete(null);
        }
    }

    private static String key(Long tenantId, Long userId, String conversationId) {
        return tenantId + "/" + userId + "/" + conversationId;
    }

    private static String key(Map<String, Object> binds) {
        return key((Long) binds.get("tenant_id"), (Long) binds.get("user_id"), (String) binds.get("conversation_id"));
    }

    private static final class LockBox {
        private boolean held;
        private final ArrayDeque<CompletableFuture<Void>> waiters = new ArrayDeque<>();

        synchronized boolean tryAcquire() {
            if (held) return false;
            held = true;
            return true;
        }

        synchronized CompletableFuture<Void> enqueue() {
            CompletableFuture<Void> granted = new CompletableFuture<>();
            waiters.add(granted);
            return granted;
        }

        synchronized void release() {
            CompletableFuture<Void> next = waiters.poll();
            if (next == null) {
                held = false;
            } else {
                next.complete(null);
            }
        }
    }

    private Mono<Void> acquire(String lockKey) {
        return Mono.defer(() -> {
            LockBox box = locks.computeIfAbsent(lockKey, ignored -> new LockBox());
            if (box.tryAcquire()) return Mono.empty();
            return Mono.fromFuture(box.enqueue()).then();
        });
    }

    private void release(String lockKey) {
        LockBox box = locks.get(lockKey);
        if (box != null) box.release();
    }

    private record Outcome(List<Map<String, Object>> rows, long rowsUpdated) {

        static final Outcome EMPTY = new Outcome(List.of(), 0);
    }

    private final class ScriptedSpec implements GenericExecuteSpec {

        private final String sql;
        private final Map<String, Object> binds = new LinkedHashMap<>();
        private final Map<String, Class<?>> nulls = new LinkedHashMap<>();

        private ScriptedSpec(String sql) {
            this.sql = sql;
        }

        @Override
        public GenericExecuteSpec bind(int index, Object value) {
            throw new UnsupportedOperationException("positional binds are not used");
        }

        @Override
        public GenericExecuteSpec bindNull(int index, Class<?> type) {
            throw new UnsupportedOperationException("positional binds are not used");
        }

        @Override
        public GenericExecuteSpec bind(String name, Object value) {
            binds.put(name, value);
            return this;
        }

        @Override
        public GenericExecuteSpec bindNull(String name, Class<?> type) {
            nulls.put(name, type);
            return this;
        }

        @Override
        public GenericExecuteSpec bindValues(List<?> values) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public GenericExecuteSpec bindValues(Map<String, ?> values) {
            binds.putAll(values);
            return this;
        }

        @Override
        public GenericExecuteSpec bindProperties(Object source) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public GenericExecuteSpec filter(org.springframework.r2dbc.core.StatementFilterFunction filter) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public GenericExecuteSpec filter(
                java.util.function.Function<? super io.r2dbc.spi.Statement, ? extends io.r2dbc.spi.Statement> filter) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public <R> RowsFetchSpec<R> map(java.util.function.Function<? super io.r2dbc.spi.Readable, R> mapping) {
            return mapped((row, ignored) -> mapping.apply(row));
        }

        @Override
        public <R> RowsFetchSpec<R> map(java.util.function.BiFunction<Row, RowMetadata, R> mapping) {
            return mapped(mapping);
        }

        @Override
        public <R> RowsFetchSpec<R> mapValue(Class<R> type) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public <R> RowsFetchSpec<R> mapProperties(Class<R> type) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public <R> Flux<R> flatMap(
                java.util.function.Function<io.r2dbc.spi.Result, org.reactivestreams.Publisher<R>> mapping) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public FetchSpec<Map<String, Object>> fetch() {
            return new ScriptedFetchSpec(this);
        }

        @Override
        public Mono<Void> then() {
            return resolveOutcome().then();
        }

        /** Resolve this statement: record it, honor any hold, then route and apply effects. */
        Mono<Outcome> resolveOutcome() {
            return Mono.defer(() -> {
                        Hold hold = takeHold(sql);
                        Mono<Outcome> body = Mono.defer(() -> route(sql, binds));
                        if (hold == null) {
                            return body;
                        }
                        return Mono.fromFuture(hold.gate).then(body);
                    })
                    .doOnSubscribe(subscription -> record(sql, binds, nulls));
        }

        private <R> RowsFetchSpec<R> mapped(java.util.function.BiFunction<Row, RowMetadata, R> mapping) {
            return new RowsFetchSpec<>() {
                @Override
                public Mono<R> one() {
                    return resolveOutcome()
                            .flatMap(outcome -> outcome.rows().isEmpty()
                                    ? Mono.<R>empty()
                                    : Mono.just(mapping.apply(
                                            new FakeRow(outcome.rows().get(0)), (RowMetadata) null)));
                }

                @Override
                public Mono<R> first() {
                    return one();
                }

                @Override
                public Flux<R> all() {
                    return resolveOutcome()
                            .flatMapMany(outcome -> Flux.fromIterable(outcome.rows().stream()
                                    .map(row -> mapping.apply(new FakeRow(row), null))
                                    .toList()));
                }
            };
        }

        private Hold takeHold(String statement) {
            synchronized (holds) {
                for (Hold hold : holds) {
                    if (statement.contains(hold.marker)) {
                        holds.remove(hold);
                        hold.routed.countDown();
                        return hold;
                    }
                }
            }
            return null;
        }

        private void record(String statement, Map<String, Object> values, Map<String, Class<?>> typedNulls) {
            synchronized (executions) {
                executions.add(new Execution(statement, Map.copyOf(values), Map.copyOf(typedNulls)));
            }
        }
    }

    private final class ScriptedFetchSpec implements FetchSpec<Map<String, Object>> {

        private final ScriptedSpec spec;

        private ScriptedFetchSpec(ScriptedSpec spec) {
            this.spec = spec;
        }

        @Override
        public Mono<Map<String, Object>> one() {
            return resolveRows().flatMap(rows -> rows.isEmpty() ? Mono.empty() : Mono.just(rows.get(0)));
        }

        @Override
        public Mono<Map<String, Object>> first() {
            return one();
        }

        @Override
        public Flux<Map<String, Object>> all() {
            return resolveRows().flatMapMany(Flux::fromIterable);
        }

        @Override
        public Mono<Long> rowsUpdated() {
            return spec.resolveOutcome().map(Outcome::rowsUpdated);
        }

        private Mono<List<Map<String, Object>>> resolveRows() {
            return spec.resolveOutcome().map(Outcome::rows);
        }
    }

    private Mono<Outcome> route(String sql, Map<String, Object> binds) {
        if (sql.startsWith("SELECT id FROM dc3_agentic.dc3_session")) {
            return lockSessionId(binds);
        }
        if (sql.startsWith("INSERT INTO dc3_agentic.dc3_session")) {
            return insertSession(binds);
        }
        if (sql.startsWith("UPDATE dc3_agentic.dc3_session")) {
            return updateSession(sql, binds);
        }
        if (sql.startsWith("SELECT id, conversation_id, title, session_ext")) {
            return selectSession(sql, binds);
        }
        if (sql.startsWith("INSERT INTO dc3_agentic.dc3_attachment")) {
            return insertAttachment(binds);
        }
        if (sql.startsWith("SELECT file_path FROM dc3_agentic.dc3_attachment")) {
            return listDeletedFilePaths(binds);
        }
        if (sql.startsWith("SELECT id, conversation_id, file_name")) {
            return selectAttachment(binds);
        }
        if (sql.startsWith("UPDATE dc3_agentic.dc3_attachment SET deleted = 1")) {
            return softDeleteAttachments(binds);
        }
        if (sql.startsWith("UPDATE dc3_agentic.dc3_message SET deleted = 1")) {
            return softDeleteMessages(binds);
        }
        if (sql.startsWith("UPDATE dc3_agentic.dc3_action SET deleted = 1")) {
            return Mono.just(new Outcome(List.of(), 0));
        }
        if (sql.startsWith("INSERT INTO dc3_agentic.dc3_action")) {
            return insertAction(binds);
        }
        if (sql.startsWith("UPDATE dc3_agentic.dc3_action")) {
            return updateAction(binds);
        }
        if (sql.startsWith("INSERT INTO dc3_agentic.dc3_message")) {
            return insertMessage(binds);
        }
        if (sql.startsWith("SELECT COALESCE(MAX(message_index)")) {
            return nextMessageIndex(binds);
        }
        if (sql.startsWith("SELECT id, action_id, conversation_id")) {
            return selectAction(binds);
        }
        if (sql.startsWith("SELECT COUNT(*) AS total FROM dc3_agentic.dc3_attachment")) {
            return countLiveAttachmentsByTenant(binds);
        }
        if (sql.startsWith("SELECT COALESCE(SUM(size)")) {
            return sumLiveAttachmentBytesByTenant(binds);
        }
        if (sql.startsWith("SELECT COUNT(*) AS total")) {
            return Mono.just(new Outcome(List.of(Map.of("total", 0L)), 0));
        }
        if (sql.startsWith("SELECT id, conversation_id, role, content")) {
            return selectMessage(binds);
        }
        // Statements outside the tested protocol (model config updates) still execute and record
        // their binds; they simply report one affected row and no data.
        return Mono.just(new Outcome(List.of(), 1));
    }

    private Mono<Outcome> lockSessionId(Map<String, Object> binds) {
        // FOR UPDATE on no matching row locks nothing, so an absent session must not queue.
        if (liveSession(binds) == null) return Mono.just(Outcome.EMPTY);
        String lockKey = key(binds);
        return acquire(lockKey).then(Mono.fromCallable(() -> {
            SessionRow row = liveSession(binds);
            return row == null ? Outcome.EMPTY : new Outcome(List.of(Map.of("id", row.id)), 0);
        }));
    }

    private Mono<Outcome> insertSession(Map<String, Object> binds) {
        Runnable script = sessionInsertViolationScript;
        if (script != null) {
            sessionInsertViolationScript = null;
            script.run();
            if (sessions.containsKey(key(binds))) {
                return Mono.error(new DataIntegrityViolationException("uq_session_tenant_user_conversation"));
            }
        }
        if (sessions.containsKey(key(binds))) {
            return Mono.error(new DataIntegrityViolationException("uq_session_tenant_user_conversation"));
        }
        SessionRow row = new SessionRow();
        row.id = sessionIds.incrementAndGet();
        row.tenantId = (Long) binds.get("tenant_id");
        row.userId = (Long) binds.get("user_id");
        row.conversationId = (String) binds.get("conversation_id");
        row.title = (String) binds.get("title");
        row.sessionExtJson = (String) binds.get("session_ext");
        row.creatorId = (Long) binds.get("creator_id");
        row.operatorId = (Long) binds.get("operator_id");
        row.createTime = (Instant) binds.get("create_time");
        row.operateTime = (Instant) binds.get("operate_time");
        row.deleted = 0;
        sessions.put(key(row.tenantId, row.userId, row.conversationId), row);
        release(key(binds));
        return Mono.just(new Outcome(List.of(), 1));
    }

    private Mono<Outcome> updateSession(String sql, Map<String, Object> binds) {
        if (sql.contains("SET deleted = 1")) {
            SessionRow row = liveSession(binds);
            if (row == null) return Mono.just(Outcome.EMPTY);
            row.deleted = 1;
            row.operatorId = (Long) binds.get("operator_id");
            row.operateTime = (Instant) binds.get("operate_time");
            release(key(row.tenantId, row.userId, row.conversationId));
            return Mono.just(new Outcome(List.of(), 1));
        }
        SessionRow row = sessionById((Long) binds.get("id"));
        // The plain update's WHERE clause guards on deleted = 0; the revive's SET clause clears
        // the tombstone flag, so distinguish by the statement tail, not by a bare substring.
        if (row == null || (sql.endsWith("deleted = 0") && row.deleted != 0)) {
            return Mono.just(Outcome.EMPTY);
        }
        if (sql.contains("SET deleted = 0")) {
            row.deleted = 0;
            row.title = (String) binds.get("title");
            row.sessionExtJson = (String) binds.get("session_ext");
            row.creatorId = (Long) binds.get("creator_id");
            row.creatorName = (String) binds.get("creator_name");
            row.createTime = (Instant) binds.get("create_time");
            row.operatorId = (Long) binds.get("operator_id");
            row.operateTime = (Instant) binds.get("operate_time");
        } else {
            row.title = (String) binds.get("title");
            row.sessionExtJson = (String) binds.get("session_ext");
            row.operatorId = (Long) binds.get("operator_id");
            row.operateTime = (Instant) binds.get("operate_time");
        }
        release(key(row.tenantId, row.userId, row.conversationId));
        return Mono.just(new Outcome(List.of(), 1));
    }

    private Mono<Outcome> selectSession(String sql, Map<String, Object> binds) {
        boolean liveOnly = sql.contains("AND deleted = 0");
        if (sql.contains("FOR UPDATE")) {
            // FOR UPDATE on no matching row locks nothing, so an absent row must not queue.
            if ((liveOnly ? liveSession(binds) : anySession(binds)) == null) {
                return Mono.just(Outcome.EMPTY);
            }
            String lockKey = key(binds);
            return acquire(lockKey).then(Mono.fromCallable(() -> {
                SessionRow row = liveOnly ? liveSession(binds) : anySession(binds);
                return row == null ? Outcome.EMPTY : new Outcome(List.of(sessionMap(row)), 0);
            }));
        }
        SessionRow row = liveSession(binds);
        return Mono.just(row == null ? Outcome.EMPTY : new Outcome(List.of(sessionMap(row)), 0));
    }

    private Mono<Outcome> insertAttachment(Map<String, Object> binds) {
        AttachmentRow row = new AttachmentRow();
        row.id = attachmentIds.incrementAndGet();
        row.tenantId = (Long) binds.get("tenant_id");
        row.userId = (Long) binds.get("user_id");
        row.conversationId = (String) binds.get("conversation_id");
        row.fileName = (String) binds.get("file_name");
        row.contentType = (String) binds.get("content_type");
        row.size = (Long) binds.get("size");
        row.filePath = (String) binds.get("file_path");
        row.creatorId = (Long) binds.get("creator_id");
        row.creatorName = (String) binds.get("creator_name");
        row.createTime = (Instant) binds.get("create_time");
        row.operatorId = (Long) binds.get("operator_id");
        row.operateTime = (Instant) binds.get("operate_time");
        row.deleted = 0;
        attachments.add(row);
        release(key(binds));
        return Mono.just(new Outcome(List.of(), 1));
    }

    private Mono<Outcome> listDeletedFilePaths(Map<String, Object> binds) {
        List<Map<String, Object>> rows = attachments.stream()
                .filter(row -> matchesConversation(row, binds) && row.deleted == 1)
                .map(row -> Map.<String, Object>of("file_path", row.filePath))
                .toList();
        return Mono.just(new Outcome(rows, 0));
    }

    private Mono<Outcome> countLiveAttachmentsByTenant(Map<String, Object> binds) {
        long total = attachments.stream()
                .filter(row -> row.tenantId.equals(binds.get("tenant_id")) && row.deleted == 0)
                .count();
        return Mono.just(new Outcome(List.of(Map.of("total", total)), 0));
    }

    private Mono<Outcome> sumLiveAttachmentBytesByTenant(Map<String, Object> binds) {
        long total = attachments.stream()
                .filter(row -> row.tenantId.equals(binds.get("tenant_id")) && row.deleted == 0)
                .mapToLong(row -> row.size == null ? 0L : row.size)
                .sum();
        return Mono.just(new Outcome(List.of(Map.of("total", total)), 0));
    }

    private Mono<Outcome> selectAttachment(Map<String, Object> binds) {
        if (binds.containsKey("file_path")) {
            AttachmentRow found = attachments.stream()
                    .filter(row -> row.filePath.equals(binds.get("file_path"))
                            && row.tenantId.equals(binds.get("tenant_id"))
                            && row.userId.equals(binds.get("user_id"))
                            && row.deleted == 0)
                    .findFirst()
                    .orElse(null);
            return Mono.just(found == null ? Outcome.EMPTY : new Outcome(List.of(attachmentMap(found)), 0));
        }
        List<Map<String, Object>> rows = attachments.stream()
                .filter(row -> matchesConversation(row, binds) && row.deleted == 0)
                .map(this::attachmentMap)
                .toList();
        return Mono.just(new Outcome(rows, 0));
    }

    private Mono<Outcome> softDeleteAttachments(Map<String, Object> binds) {
        long affected = attachments.stream()
                .filter(row -> matchesConversation(row, binds) && row.deleted == 0)
                .peek(row -> row.deleted = 1)
                .count();
        return Mono.just(new Outcome(List.of(), affected));
    }

    private Mono<Outcome> softDeleteMessages(Map<String, Object> binds) {
        long affected = messages.stream()
                .filter(row -> matchesConversation(row, binds) && row.deleted == 0)
                .peek(row -> row.deleted = 1)
                .count();
        return Mono.just(new Outcome(List.of(), affected));
    }

    private Mono<Outcome> insertMessage(Map<String, Object> binds) {
        MessageRow row = new MessageRow();
        row.id = messageIds.incrementAndGet();
        row.tenantId = (Long) binds.get("tenant_id");
        row.userId = (Long) binds.get("user_id");
        row.conversationId = (String) binds.get("conversation_id");
        row.role = (String) binds.get("role");
        row.content = (String) binds.get("content");
        row.model = (String) binds.get("model");
        row.messageIndex = (Long) binds.get("message_index");
        row.status = (Byte) binds.get("status");
        row.createTime = (Instant) binds.get("create_time");
        row.deleted = 0;
        messages.add(row);
        release(key(binds));
        return Mono.just(new Outcome(List.of(), 1));
    }

    private Mono<Outcome> nextMessageIndex(Map<String, Object> binds) {
        long next = messages.stream()
                        .filter(row -> matchesConversation(row, binds) && row.deleted == 0)
                        .mapToLong(row -> row.messageIndex)
                        .max()
                        .orElse(-1L)
                + 1;
        return Mono.just(new Outcome(List.of(Map.of("next_index", next)), 0));
    }

    private Mono<Outcome> selectMessage(Map<String, Object> binds) {
        MessageRow found = messages.stream()
                .filter(row -> matchesConversation(row, binds)
                        && row.deleted == 0
                        && row.messageIndex.equals(binds.get("message_index")))
                .findFirst()
                .orElse(null);
        if (found == null) return Mono.just(Outcome.EMPTY);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", found.id);
        row.put("conversation_id", found.conversationId);
        row.put("role", found.role);
        row.put("content", found.content);
        row.put("model", found.model);
        row.put("message_index", found.messageIndex);
        row.put("status", found.status);
        row.put("tenant_id", found.tenantId);
        row.put("user_id", found.userId);
        row.put("remark", "");
        row.put("creator_id", found.tenantId);
        row.put("creator_name", "");
        row.put("create_time", found.createTime);
        row.put("operator_id", found.tenantId);
        row.put("operator_name", "");
        row.put("operate_time", found.createTime);
        return Mono.just(new Outcome(List.of(row), 0));
    }

    private SessionRow liveSession(Map<String, Object> binds) {
        SessionRow row = anySession(binds);
        return row != null && row.deleted == 0 ? row : null;
    }

    private Mono<Outcome> insertAction(Map<String, Object> binds) {
        ActionRow row = new ActionRow();
        row.id = actionIds.incrementAndGet();
        row.actionId = (String) binds.get("action_id");
        row.conversationId = (String) binds.get("conversation_id");
        row.actionType = (String) binds.get("action_type");
        row.title = (String) binds.get("title");
        row.description = (String) binds.get("description");
        row.payload = (String) binds.get("payload");
        row.status = (Byte) binds.get("status");
        row.tenantId = (Long) binds.get("tenant_id");
        row.userId = (Long) binds.get("user_id");
        row.createTime = (Instant) binds.get("create_time");
        row.expireTime = (Instant) binds.get("expire_time");
        row.deleted = 0;
        actions.add(row);
        return Mono.just(new Outcome(List.of(), 1));
    }

    private Mono<Outcome> updateAction(Map<String, Object> binds) {
        ActionRow row = actions.stream()
                .filter(candidate -> candidate.actionId.equals(binds.get("action_id"))
                        && candidate.tenantId.equals(binds.get("tenant_id"))
                        && candidate.userId.equals(binds.get("user_id"))
                        && candidate.deleted == 0)
                .findFirst()
                .orElse(null);
        if (row == null) return Mono.just(Outcome.EMPTY);
        if (binds.containsKey("status")) row.status = (Byte) binds.get("status");
        return Mono.just(new Outcome(List.of(), 1));
    }

    private Mono<Outcome> selectAction(Map<String, Object> binds) {
        ActionRow found = actions.stream()
                .filter(candidate -> candidate.actionId.equals(binds.get("action_id"))
                        && candidate.tenantId.equals(binds.get("tenant_id"))
                        && candidate.userId.equals(binds.get("user_id"))
                        && candidate.deleted == 0)
                .findFirst()
                .orElse(null);
        if (found == null) return Mono.just(Outcome.EMPTY);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", found.id);
        row.put("action_id", found.actionId);
        row.put("conversation_id", found.conversationId);
        row.put("action_type", found.actionType);
        row.put("title", found.title);
        row.put("description", found.description);
        row.put("payload", found.payload);
        row.put("status", found.status);
        row.put("expire_time", found.expireTime);
        row.put("tenant_id", found.tenantId);
        row.put("user_id", found.userId);
        row.put("remark", "");
        row.put("creator_id", found.tenantId);
        row.put("creator_name", "");
        row.put("create_time", found.createTime);
        row.put("operator_id", found.tenantId);
        row.put("operator_name", "");
        row.put("operate_time", found.createTime);
        return Mono.just(new Outcome(List.of(row), 0));
    }

    private SessionRow anySession(Map<String, Object> binds) {
        return sessions.get(key(binds));
    }

    private SessionRow sessionById(Long id) {
        return sessions.values().stream()
                .filter(row -> row.id.equals(id))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesConversation(AttachmentRow row, Map<String, Object> binds) {
        return row.conversationId.equals(binds.get("conversation_id"))
                && row.tenantId.equals(binds.get("tenant_id"))
                && row.userId.equals(binds.get("user_id"));
    }

    private boolean matchesConversation(MessageRow row, Map<String, Object> binds) {
        return row.conversationId.equals(binds.get("conversation_id"))
                && row.tenantId.equals(binds.get("tenant_id"))
                && row.userId.equals(binds.get("user_id"));
    }

    private Map<String, Object> sessionMap(SessionRow row) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", row.id);
        values.put("conversation_id", row.conversationId);
        values.put("title", row.title);
        values.put("session_ext", row.sessionExtJson);
        values.put("tenant_id", row.tenantId);
        values.put("user_id", row.userId);
        values.put("remark", "");
        values.put("creator_id", row.creatorId);
        values.put("creator_name", row.creatorName);
        values.put("create_time", row.createTime);
        values.put("operator_id", row.operatorId);
        values.put("operator_name", row.operatorName);
        values.put("operate_time", row.operateTime);
        values.put("deleted", row.deleted);
        return values;
    }

    private Map<String, Object> attachmentMap(AttachmentRow row) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", row.id);
        values.put("conversation_id", row.conversationId);
        values.put("file_name", row.fileName);
        values.put("content_type", row.contentType);
        values.put("size", row.size);
        values.put("file_path", row.filePath);
        values.put("tenant_id", row.tenantId);
        values.put("user_id", row.userId);
        values.put("remark", "");
        values.put("creator_id", row.creatorId);
        values.put("creator_name", row.creatorName);
        values.put("create_time", row.createTime);
        values.put("operator_id", row.operatorId);
        values.put("operator_name", row.operatorName);
        values.put("operate_time", row.operateTime);
        values.put("deleted", row.deleted);
        return values;
    }

    /** Minimal map-backed {@link Row} with the numeric coercions the store mappers rely on. */
    private static final class FakeRow implements Row {

        private final Map<String, Object> values;

        private FakeRow(Map<String, Object> values) {
            this.values = values;
        }

        @Override
        public <T> T get(String name, Class<T> type) {
            Object value = values.get(name);
            if (value == null) return null;
            if (type.isInstance(value)) return type.cast(value);
            if (value instanceof Number number) {
                if (type == Long.class) return type.cast(number.longValue());
                if (type == Integer.class) return type.cast(number.intValue());
                if (type == Double.class) return type.cast(number.doubleValue());
                if (type == Number.class) return type.cast(number);
            }
            if (type == String.class) return type.cast(value.toString());
            throw new IllegalStateException("unsupported column type: " + name);
        }

        @Override
        public <T> T get(int index, Class<T> type) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public RowMetadata getMetadata() {
            throw new UnsupportedOperationException("not used");
        }
    }
}
