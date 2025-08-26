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
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

/**
 * 证书导入工具类
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
public class KeyStoreUtil {

    private KeyStoreUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

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

    private static Resource getResource(ResourceLoader resourceLoader, String[] resourcePaths, String fileName) {
        for (String path : resourcePaths) {
            Resource resource = resourceLoader.getResource(path + fileName);
            if (resource.exists()) {
                return resource;
            }
        }
        throw new NotFoundException("Certificate file '{}' doesn't exist", fileName);
    }

    private static void importKeystore(InputStream crtInputStream, String crtAliasName, String passphrase) throws Exception {
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

        try (OutputStream outputStream = Files.newOutputStream(path)) {
            BufferedInputStream bis = new BufferedInputStream(crtInputStream);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            while (bis.available() > 0) {
                Certificate cert = cf.generateCertificate(bis);
                keystore.setCertificateEntry(crtAliasName, cert);
            }
            keystore.store(outputStream, passphraseArray);
        }
        log.info("Certificate '{}' imported successfully", crtAliasName);
    }
}