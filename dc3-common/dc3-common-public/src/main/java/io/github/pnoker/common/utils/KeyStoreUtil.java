/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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