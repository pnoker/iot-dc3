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

package io.github.pnoker.common.utils;

import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Collection;

/**
 * Imports SSL certificates into the JDK default keystore.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
public class KeyStoreUtil {

    private KeyStoreUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Import a certificate into the JDK default cacerts keystore. The certificate is
     * resolved from {@code classpath:/ssl/} then {@code file:./ssl/}, and the keystore
     * passphrase is the JDK default {@code changeit}.
     *
     * @param crtFileName  certificate file name
     * @param crtNameAlias alias under which to store the certificate
     */
    public static void importKeystore(String crtFileName, String crtNameAlias) {
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        String[] resourcePaths = new String[]{"classpath:/ssl/", "file:./ssl/"};
        String passphrase = "changeit";
        try (InputStream inputStream = getResource(resourceLoader, resourcePaths, crtFileName).getInputStream()) {
            KeyStoreUtil.importKeystore(inputStream, crtNameAlias, passphrase);
        } catch (Exception e) {
            throw new SecurityException(e);
        }
    }

    /**
     * Locate a resource by trying each base path in turn, returning the first that
     * exists.
     *
     * @param resourceLoader the resource loader
     * @param resourcePaths  base paths to try in order
     * @param fileName       the file name to append to each base path
     * @return the first existing resource
     * @throws NotFoundException if no path yields an existing resource
     */
    private static Resource getResource(ResourceLoader resourceLoader, String[] resourcePaths, String fileName) {
        for (String path : resourcePaths) {
            Resource resource = resourceLoader.getResource(path + fileName);
            if (resource.exists()) {
                return resource;
            }
        }
        throw new NotFoundException("Certificate file '{}' doesn't exist", fileName);
    }

    /**
     * Load the JDK cacerts keystore, skip the import when the alias already exists, and
     * otherwise write the certificate(s) from the stream under the alias.
     *
     * @param crtInputStream certificate input stream
     * @param crtAliasName   alias to store under
     * @param passphrase     keystore passphrase
     * @throws Exception on keystore load/store or certificate parsing failure
     */
    private static void importKeystore(InputStream crtInputStream, String crtAliasName, String passphrase)
            throws Exception {
        log.info("Importing certificate '{}'", crtAliasName);
        final char separator = File.separatorChar;
        final char[] passphraseArray = passphrase.toCharArray();
        File securityFile = new File(System.getProperty("java.home") + separator + "lib" + separator + "security");
        File keyStoreFile = new File(securityFile, "cacerts");
        Path path = keyStoreFile.toPath();
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream inputStream = Files.newInputStream(path)) {
            keystore.load(inputStream, passphraseArray);
            if (keystore.containsAlias(crtAliasName)) {
                log.info("Skip import, certificate '{}' already exists", crtAliasName);
                return;
            }
        }

        // Parse all certificates up front, so a malformed stream fails before cacerts is touched.
        Collection<? extends Certificate> certificates;
        try (BufferedInputStream bis = new BufferedInputStream(crtInputStream)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            certificates = cf.generateCertificates(bis);
        }
        if (certificates.isEmpty()) {
            throw new NotFoundException("No X.509 certificate found in stream for alias '{}'", crtAliasName);
        }
        int index = 0;
        for (Certificate cert : certificates) {
            // Use a distinct alias per certificate so multiple certs are not collapsed into one entry.
            String alias = certificates.size() == 1 ? crtAliasName : crtAliasName + "_" + index++;
            keystore.setCertificateEntry(alias, cert);
        }

        // Write to a temp file and atomically replace cacerts, so a store() failure never
        // leaves the JVM-wide trust store truncated or partially written.
        Path tempPath = Files.createTempFile(path.getParent(), "cacerts", ".tmp");
        try {
            try (OutputStream outputStream = Files.newOutputStream(tempPath)) {
                keystore.store(outputStream, passphraseArray);
            }
            Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            Files.deleteIfExists(tempPath);
            throw e;
        }
        log.info("Certificate '{}' imported successfully", crtAliasName);
    }

}
