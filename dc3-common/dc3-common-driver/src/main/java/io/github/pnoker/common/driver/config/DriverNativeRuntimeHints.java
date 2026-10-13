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
package io.github.pnoker.common.driver.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

/**
 * GraalVM native-image hints for the driver SDK's reflective runtime paths.
 * The native build passes the third-party reachability-metadata repository
 * through -H:ConfigurationFileDirectories, which disables classpath metadata
 * scanning - so these registrations must live in Spring AOT hints to reach
 * the image.
 *
 * <p>Registered families:
 * <ul>
 *   <li>SDK classpath resources: the driver profile defaults and the SLF4J /
 *       gRPC ServiceLoader descriptors, plus the sqlite-jdbc JNI library the
 *       point-value buffer extracts at runtime.</li>
 *   <li>Third-party reflective stacks shared with the centers (logback
 *       binding, grpc-java counters, grpc-netty epoll options): copied as a
 *       bounded subset from Dc3NativeRuntimeHints because drivers do not
 *       depend on dc3-common-web and that module carries web-only
 *       classpath items (R2DBC SPI, swagger webjars, SpEL beans).</li>
 *   <li>Caffeine policy classes: LocalCacheFactory resolves its
 *       implementation class by Class.forName from the cache configuration
 *       and reads the static FACTORY field reflectively.</li>
 *   <li>Quartz jobs: the scheduler instantiates job classes reflectively.</li>
 *   <li>@Dc3Listener receivers: Dc3ListenerProcessor invokes the listener
 *       method reflectively.</li>
 * </ul>
 *
 * <p>Consciously NOT registered here (already covered transitively): the
 * Quartz core classes (QuartzNativeRuntimeHints in dc3-common-quartz, a
 * direct SDK dependency) and the entity Jackson surface
 * (ModelNativeRuntimeHints in dc3-common-model, a transitive dependency).
 *
 * @author pnoker
 */
public class DriverNativeRuntimeHints implements RuntimeHintsRegistrar {

    /**
     * Copied from DataNativeRuntimeHints (dc3-common-data): drivers do not
     * depend on that module and the two share no common upstream that could
     * host the family. Keep the two lists in sync when Caffeine upgrades.
     */
    private static final String[] CAFFEINE_POLICY_CLASSES = {
        "com.github.benmanes.caffeine.cache.PDA",
        "com.github.benmanes.caffeine.cache.PDAMS",
        "com.github.benmanes.caffeine.cache.PDAMW",
        "com.github.benmanes.caffeine.cache.PDAR",
        "com.github.benmanes.caffeine.cache.PDARMS",
        "com.github.benmanes.caffeine.cache.PDARMW",
        "com.github.benmanes.caffeine.cache.PDAW",
        "com.github.benmanes.caffeine.cache.PDAWMS",
        "com.github.benmanes.caffeine.cache.PDAWMW",
        "com.github.benmanes.caffeine.cache.PDAWR",
        "com.github.benmanes.caffeine.cache.PDAWRMS",
        "com.github.benmanes.caffeine.cache.PDAWRMW",
        "com.github.benmanes.caffeine.cache.PDMS",
        "com.github.benmanes.caffeine.cache.PDMW",
        "com.github.benmanes.caffeine.cache.PDR",
        "com.github.benmanes.caffeine.cache.PDRMS",
        "com.github.benmanes.caffeine.cache.PDRMW",
        "com.github.benmanes.caffeine.cache.PDW",
        "com.github.benmanes.caffeine.cache.PDWMS",
        "com.github.benmanes.caffeine.cache.PDWMW",
        "com.github.benmanes.caffeine.cache.PDWR",
        "com.github.benmanes.caffeine.cache.PDWRMS",
        "com.github.benmanes.caffeine.cache.PDWRMW",
        "com.github.benmanes.caffeine.cache.PSA",
        "com.github.benmanes.caffeine.cache.PSAMS",
        "com.github.benmanes.caffeine.cache.PSAMW",
        "com.github.benmanes.caffeine.cache.PSAR",
        "com.github.benmanes.caffeine.cache.PSARMS",
        "com.github.benmanes.caffeine.cache.PSARMW",
        "com.github.benmanes.caffeine.cache.PSAW",
        "com.github.benmanes.caffeine.cache.PSAWMS",
        "com.github.benmanes.caffeine.cache.PSAWMW",
        "com.github.benmanes.caffeine.cache.PSAWR",
        "com.github.benmanes.caffeine.cache.PSAWRMS",
        "com.github.benmanes.caffeine.cache.PSAWRMW",
        "com.github.benmanes.caffeine.cache.PSMS",
        "com.github.benmanes.caffeine.cache.PSMW",
        "com.github.benmanes.caffeine.cache.PSR",
        "com.github.benmanes.caffeine.cache.PSRMS",
        "com.github.benmanes.caffeine.cache.PSRMW",
        "com.github.benmanes.caffeine.cache.PSW",
        "com.github.benmanes.caffeine.cache.PSWMS",
        "com.github.benmanes.caffeine.cache.PSWMW",
        "com.github.benmanes.caffeine.cache.PSWR",
        "com.github.benmanes.caffeine.cache.PSWRMS",
        "com.github.benmanes.caffeine.cache.PSWRMW",
        "com.github.benmanes.caffeine.cache.PWA",
        "com.github.benmanes.caffeine.cache.PWAMS",
        "com.github.benmanes.caffeine.cache.PWAMW",
        "com.github.benmanes.caffeine.cache.PWAR",
        "com.github.benmanes.caffeine.cache.PWARMS",
        "com.github.benmanes.caffeine.cache.PWARMW",
        "com.github.benmanes.caffeine.cache.PWAW",
        "com.github.benmanes.caffeine.cache.PWAWMS",
        "com.github.benmanes.caffeine.cache.PWAWMW",
        "com.github.benmanes.caffeine.cache.PWAWR",
        "com.github.benmanes.caffeine.cache.PWAWRMS",
        "com.github.benmanes.caffeine.cache.PWAWRMW",
        "com.github.benmanes.caffeine.cache.PWMS",
        "com.github.benmanes.caffeine.cache.PWMW",
        "com.github.benmanes.caffeine.cache.PWR",
        "com.github.benmanes.caffeine.cache.PWRMS",
        "com.github.benmanes.caffeine.cache.PWRMW",
        "com.github.benmanes.caffeine.cache.PWW",
        "com.github.benmanes.caffeine.cache.PWWMS",
        "com.github.benmanes.caffeine.cache.PWWMW",
        "com.github.benmanes.caffeine.cache.PWWR",
        "com.github.benmanes.caffeine.cache.PWWRMS",
        "com.github.benmanes.caffeine.cache.PWWRMW",
        "com.github.benmanes.caffeine.cache.SIA",
        "com.github.benmanes.caffeine.cache.SIAR",
        "com.github.benmanes.caffeine.cache.SIAW",
        "com.github.benmanes.caffeine.cache.SIAWR",
        "com.github.benmanes.caffeine.cache.SIL",
        "com.github.benmanes.caffeine.cache.SILA",
        "com.github.benmanes.caffeine.cache.SILAR",
        "com.github.benmanes.caffeine.cache.SILAW",
        "com.github.benmanes.caffeine.cache.SILAWR",
        "com.github.benmanes.caffeine.cache.SILMS",
        "com.github.benmanes.caffeine.cache.SILMSA",
        "com.github.benmanes.caffeine.cache.SILMSAR",
        "com.github.benmanes.caffeine.cache.SILMSAW",
        "com.github.benmanes.caffeine.cache.SILMSAWR",
        "com.github.benmanes.caffeine.cache.SILMSR",
        "com.github.benmanes.caffeine.cache.SILMSW",
        "com.github.benmanes.caffeine.cache.SILMSWR",
        "com.github.benmanes.caffeine.cache.SILMW",
        "com.github.benmanes.caffeine.cache.SILMWA",
        "com.github.benmanes.caffeine.cache.SILMWAR",
        "com.github.benmanes.caffeine.cache.SILMWAW",
        "com.github.benmanes.caffeine.cache.SILMWAWR",
        "com.github.benmanes.caffeine.cache.SILMWR",
        "com.github.benmanes.caffeine.cache.SILMWW",
        "com.github.benmanes.caffeine.cache.SILMWWR",
        "com.github.benmanes.caffeine.cache.SILR",
        "com.github.benmanes.caffeine.cache.SILS",
        "com.github.benmanes.caffeine.cache.SILSA",
        "com.github.benmanes.caffeine.cache.SILSAR",
        "com.github.benmanes.caffeine.cache.SILSAW",
        "com.github.benmanes.caffeine.cache.SILSAWR",
        "com.github.benmanes.caffeine.cache.SILSMS",
        "com.github.benmanes.caffeine.cache.SILSMSA",
        "com.github.benmanes.caffeine.cache.SILSMSAR",
        "com.github.benmanes.caffeine.cache.SILSMSAW",
        "com.github.benmanes.caffeine.cache.SILSMSR",
        "com.github.benmanes.caffeine.cache.SILSMSW",
        "com.github.benmanes.caffeine.cache.SILSMSWR",
        "com.github.benmanes.caffeine.cache.SILSMW",
        "com.github.benmanes.caffeine.cache.SILSMWA",
        "com.github.benmanes.caffeine.cache.SILSMWAR",
        "com.github.benmanes.caffeine.cache.SILSMWAW",
        "com.github.benmanes.caffeine.cache.SILSMWR",
        "com.github.benmanes.caffeine.cache.SILSMWW",
        "com.github.benmanes.caffeine.cache.SILSMWWR",
        "com.github.benmanes.caffeine.cache.SILSR",
        "com.github.benmanes.caffeine.cache.SILSW",
        "com.github.benmanes.caffeine.cache.SILSWR",
        "com.github.benmanes.caffeine.cache.SILW",
        "com.github.benmanes.caffeine.cache.SILWR",
        "com.github.benmanes.caffeine.cache.SIMS",
        "com.github.benmanes.caffeine.cache.SIMSA",
        "com.github.benmanes.caffeine.cache.SIMSAR",
        "com.github.benmanes.caffeine.cache.SIMSAW",
        "com.github.benmanes.caffeine.cache.SIMSAWR",
        "com.github.benmanes.caffeine.cache.SIMSR",
        "com.github.benmanes.caffeine.cache.SIMSW",
        "com.github.benmanes.caffeine.cache.SIMSWR",
        "com.github.benmanes.caffeine.cache.SIMW",
        "com.github.benmanes.caffeine.cache.SIMWA",
        "com.github.benmanes.caffeine.cache.SIMWAR",
        "com.github.benmanes.caffeine.cache.SIMWAW",
        "com.github.benmanes.caffeine.cache.SIMWAWR",
        "com.github.benmanes.caffeine.cache.SIMWR",
        "com.github.benmanes.caffeine.cache.SIMWW",
        "com.github.benmanes.caffeine.cache.SIMWWR",
        "com.github.benmanes.caffeine.cache.SIR",
        "com.github.benmanes.caffeine.cache.SIS",
        "com.github.benmanes.caffeine.cache.SISA",
        "com.github.benmanes.caffeine.cache.SISAR",
        "com.github.benmanes.caffeine.cache.SISAW",
        "com.github.benmanes.caffeine.cache.SISAWR",
        "com.github.benmanes.caffeine.cache.SISMS",
        "com.github.benmanes.caffeine.cache.SISMSA",
        "com.github.benmanes.caffeine.cache.SISMSAR",
        "com.github.benmanes.caffeine.cache.SISMSAW",
        "com.github.benmanes.caffeine.cache.SISMSAWR",
        "com.github.benmanes.caffeine.cache.SISMSR",
        "com.github.benmanes.caffeine.cache.SISMSW",
        "com.github.benmanes.caffeine.cache.SISMSWR",
        "com.github.benmanes.caffeine.cache.SISMW",
        "com.github.benmanes.caffeine.cache.SISMWA",
        "com.github.benmanes.caffeine.cache.SISMWAR",
        "com.github.benmanes.caffeine.cache.SISMWAW",
        "com.github.benmanes.caffeine.cache.SISMWAWR",
        "com.github.benmanes.caffeine.cache.SISMWR",
        "com.github.benmanes.caffeine.cache.SISMWW",
        "com.github.benmanes.caffeine.cache.SISMWWR",
        "com.github.benmanes.caffeine.cache.SISR",
        "com.github.benmanes.caffeine.cache.SISW",
        "com.github.benmanes.caffeine.cache.SISWR",
        "com.github.benmanes.caffeine.cache.SIW",
        "com.github.benmanes.caffeine.cache.SIWR",
        "com.github.benmanes.caffeine.cache.SSA",
        "com.github.benmanes.caffeine.cache.SSAR",
        "com.github.benmanes.caffeine.cache.SSAW",
        "com.github.benmanes.caffeine.cache.SSAWR",
        "com.github.benmanes.caffeine.cache.SSL",
        "com.github.benmanes.caffeine.cache.SSLA",
        "com.github.benmanes.caffeine.cache.SSLAR",
        "com.github.benmanes.caffeine.cache.SSLAW",
        "com.github.benmanes.caffeine.cache.SSLAWR",
        "com.github.benmanes.caffeine.cache.SSLMS",
        "com.github.benmanes.caffeine.cache.SSLMSA",
        "com.github.benmanes.caffeine.cache.SSLMSAR",
        "com.github.benmanes.caffeine.cache.SSLMSAW",
        "com.github.benmanes.caffeine.cache.SSLMSAWR",
        "com.github.benmanes.caffeine.cache.SSLMSR",
        "com.github.benmanes.caffeine.cache.SSLMSW",
        "com.github.benmanes.caffeine.cache.SSLMSWR",
        "com.github.benmanes.caffeine.cache.SSLMW",
        "com.github.benmanes.caffeine.cache.SSLMWA",
        "com.github.benmanes.caffeine.cache.SSLMWAR",
        "com.github.benmanes.caffeine.cache.SSLMWAW",
        "com.github.benmanes.caffeine.cache.SSLMWAWR",
        "com.github.benmanes.caffeine.cache.SSLMWR",
        "com.github.benmanes.caffeine.cache.SSLMWW",
        "com.github.benmanes.caffeine.cache.SSLMWWR",
        "com.github.benmanes.caffeine.cache.SSLR",
        "com.github.benmanes.caffeine.cache.SSLS",
        "com.github.benmanes.caffeine.cache.SSLSA",
        "com.github.benmanes.caffeine.cache.SSLSAR",
        "com.github.benmanes.caffeine.cache.SSLSAW",
        "com.github.benmanes.caffeine.cache.SSLSAWR",
        "com.github.benmanes.caffeine.cache.SSLSMS",
        "com.github.benmanes.caffeine.cache.SSLSMSA",
        "com.github.benmanes.caffeine.cache.SSLSMSAR",
        "com.github.benmanes.caffeine.cache.SSLSMSAW",
        "com.github.benmanes.caffeine.cache.SSLSMSAWR",
        "com.github.benmanes.caffeine.cache.SSLSMSR",
        "com.github.benmanes.caffeine.cache.SSLSMSW",
        "com.github.benmanes.caffeine.cache.SSLSMSWR",
        "com.github.benmanes.caffeine.cache.SSLSMW",
        "com.github.benmanes.caffeine.cache.SSLSMWA",
        "com.github.benmanes.caffeine.cache.SSLSMWAR",
        "com.github.benmanes.caffeine.cache.SSLSMWAW",
        "com.github.benmanes.caffeine.cache.SSLSMWR",
        "com.github.benmanes.caffeine.cache.SSLSMWW",
        "com.github.benmanes.caffeine.cache.SSLSMWWR",
        "com.github.benmanes.caffeine.cache.SSR",
        "com.github.benmanes.caffeine.cache.SSS",
        "com.github.benmanes.caffeine.cache.SSSA",
        "com.github.benmanes.caffeine.cache.SSSAR",
        "com.github.benmanes.caffeine.cache.SSSAW",
        "com.github.benmanes.caffeine.cache.SSSAWR",
        "com.github.benmanes.caffeine.cache.SSSMS",
        "com.github.benmanes.caffeine.cache.SSSMSA",
        "com.github.benmanes.caffeine.cache.SSSMSAR",
        "com.github.benmanes.caffeine.cache.SSSMSAW",
        "com.github.benmanes.caffeine.cache.SSSMSAWR",
        "com.github.benmanes.caffeine.cache.SSSMSR",
        "com.github.benmanes.caffeine.cache.SSSMSW",
        "com.github.benmanes.caffeine.cache.SSSMSWR",
        "com.github.benmanes.caffeine.cache.SSSMW",
        "com.github.benmanes.caffeine.cache.SSSMWA",
        "com.github.benmanes.caffeine.cache.SSSMWAR",
        "com.github.benmanes.caffeine.cache.SSSMWAW",
        "com.github.benmanes.caffeine.cache.SSSMWAWR",
        "com.github.benmanes.caffeine.cache.SSSMWR",
        "com.github.benmanes.caffeine.cache.SSSMWW",
        "com.github.benmanes.caffeine.cache.SSSMWWR",
        "com.github.benmanes.caffeine.cache.SSSR",
        "com.github.benmanes.caffeine.cache.SSSW",
        "com.github.benmanes.caffeine.cache.SSSWR",
        "com.github.benmanes.caffeine.cache.SSW",
        "com.github.benmanes.caffeine.cache.SSWR",
    };

    /**
     * Quartz jobs scheduled by the driver SDK: the scheduler instantiates
     * these classes reflectively at trigger time.
     */
    private static final String[] DRIVER_JOBS = {
        "io.github.pnoker.common.driver.job.BufferRepublishScheduleJob",
        "io.github.pnoker.common.driver.job.DeviceHealthScheduleJob",
        "io.github.pnoker.common.driver.job.DriverCustomScheduleJob",
        "io.github.pnoker.common.driver.job.DriverHealthScheduleJob",
        "io.github.pnoker.common.driver.job.DriverLeaseRenewScheduleJob",
        "io.github.pnoker.common.driver.job.DriverReadScheduleJob"
    };

    /**
     * @Dc3Listener receivers of the driver SDK: Dc3ListenerProcessor invokes
     * the listener method reflectively on RabbitMQ delivery.
     */
    private static final String[] DRIVER_LISTENERS = {
        "io.github.pnoker.common.driver.receiver.rabbit.CommandReceiver",
        "io.github.pnoker.common.driver.receiver.rabbit.MetadataReceiver",
        "io.github.pnoker.common.driver.receiver.rabbit.PointCommandReceiver"
    };

    /**
     * JDBC dialect drivers of the database driver batch, instantiated
     * reflectively by DriverManager / HikariCP. String-based references:
     * each class exists only on its own driver's classpath.
     */
    private static final String[] JDBC_DRIVER_CLASSES = {
        "com.mysql.cj.jdbc.Driver",
        "org.postgresql.Driver",
        "com.microsoft.sqlserver.jdbc.SQLServerDriver",
        "oracle.jdbc.driver.OracleDriver"
    };

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // Driver profile defaults shipped inside this SDK jar; without the
        // pattern the gRPC client channel config never loads in a native image.
        hints.resources().registerPattern("application-driver.yml");

        // Logging configuration shipped in dc3-common-log; without this
        // pattern native services silently fall back to the logback default.
        hints.resources().registerPattern("logback.xml");

        // SLF4J -> logback binding: logback-classic registers its provider
        // via a META-INF/services descriptor and ships no native-image
        // config of its own. Dropped from the image, the binding degrades to
        // NOP and kills the logging bridge at startup.
        hints.resources().registerPattern("META-INF/services/org.slf4j.spi.SLF4JServiceProvider");
        hints.reflection()
                .registerType(
                        TypeReference.of("ch.qos.logback.classic.spi.LogbackServiceProvider"),
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);

        // gRPC resolves name resolvers (dns:// etc.) through ServiceLoader,
        // and grpc-java's ReflectionLongAdderCounter instantiates LongAdder
        // reflectively when a server/channel tracer boots.
        hints.resources().registerPattern("META-INF/services/io.grpc.NameResolverProvider");
        hints.reflection()
                .registerType(
                        TypeReference.of("java.util.concurrent.atomic.LongAdder"),
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);

        // gRPC-netty's Utils.getEpollChannelOption reflectively reads static
        // final ChannelOption fields from the epoll option class via
        // Class.forName + getField; every driver runs a gRPC channel.
        hints.reflection()
                .registerType(
                        TypeReference.of("io.netty.channel.epoll.EpollChannelOption"),
                        MemberCategory.PUBLIC_FIELDS,
                        MemberCategory.DECLARED_FIELDS);
        hints.reflection()
                .registerType(
                        TypeReference.of("jdk.net.ExtendedSocketOptions"),
                        MemberCategory.PUBLIC_FIELDS,
                        MemberCategory.DECLARED_FIELDS);

        // The point-value buffer persists through sqlite-jdbc: the JNI
        // library is extracted from org/sqlite/native at runtime and the
        // JDBC driver class is instantiated by HikariCP's driver-class
        // resolution. The reachability metadata repository carries
        // sqlite-jdbc entries; these are the explicit belt-and-braces.
        hints.resources().registerPattern("org/sqlite/native/**");
        hints.reflection().registerType(TypeReference.of("org.sqlite.JDBC"), MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);

        // Pre-seeded for the JDBC driver batch: DriverManager discovers
        // drivers through this ServiceLoader descriptor.
        hints.resources().registerPattern("META-INF/services/java.sql.Driver");

        // JDBC driver batch (mysql / postgresql / sqlserver / oracle): the
        // dialect drivers are instantiated reflectively by DriverManager /
        // HikariCP. String-based TypeReferences - the classes only exist on
        // the JDBC drivers' classpaths, and hints for absent classes are
        // ignored by native-image.
        for (String name : JDBC_DRIVER_CLASSES) {
            hints.reflection().registerType(TypeReference.of(name), MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
        }

        for (String name : CAFFEINE_POLICY_CLASSES) {
            hints.reflection()
                    .registerType(
                            TypeReference.of(name),
                            MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                            MemberCategory.DECLARED_FIELDS);
        }
        for (String name : DRIVER_JOBS) {
            hints.reflection()
                    .registerType(
                            TypeReference.of(name),
                            MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        }
        for (String name : DRIVER_LISTENERS) {
            hints.reflection()
                    .registerType(
                            TypeReference.of(name),
                            MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                            MemberCategory.INVOKE_PUBLIC_METHODS,
                            MemberCategory.INVOKE_DECLARED_METHODS);
        }
    }
}
