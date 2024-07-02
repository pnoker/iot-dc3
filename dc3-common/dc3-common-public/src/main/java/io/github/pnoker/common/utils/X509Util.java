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
import io.github.pnoker.common.exception.ConnectorException;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemReader;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
public class X509Util {

    private X509Util() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    // TODO: 2023.10.16 此处有问题, 目前为不可用状态
    public static SSLSocketFactory getSSLSocketFactory(final String caCrtFile, final String crtFile, final String keyFile, final String password) {
        try {
            Security.addProvider(new BouncyCastleProvider());

            // load CA certificate
            X509Certificate caCert = loadCertificate(caCrtFile);
            // CA certificate is used to authenticate server
            KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            caKeyStore.load(null, null);
            caKeyStore.setCertificateEntry("cacertfile", caCert);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(caKeyStore);

            // load client certificate
            X509Certificate cert = loadCertificate(crtFile);
            // load client private key
            KeyPair key = loadCertificateWithPassword(keyFile, password);
            // client key and certificates are sent to server, so it can authenticate us
            KeyStore certKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            certKeyStore.load(null, null);
            certKeyStore.setCertificateEntry("certfile", cert);
            certKeyStore.setKeyEntry("keyfile", key.getPrivate(), password.toCharArray(), new java.security.cert.Certificate[]{cert});
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(certKeyStore, password.toCharArray());

            // finally, create SSL socket factory
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

            return context.getSocketFactory();
        } catch (Exception e) {
            throw new ConnectorException(e.getMessage());
        }
    }

    // TODO: 2023.10.16 此处有问题, 目前为不可用状态
    private static <T> T loadCertificate(String caCrtFile) throws IOException {
        return loadCertificateWithPassword(caCrtFile, null);
    }

    // TODO: 2023.10.16 此处有问题, 目前为不可用状态
    @SuppressWarnings("unchecked")
    private static <T> T loadCertificateWithPassword(String caCrtFile, String password) throws IOException {
        PemReader reader = null;
        try {
            String classPath = "classpath:";
            if (caCrtFile.startsWith(classPath)) {
                InputStream inputStream = X509Util.class.getResourceAsStream(caCrtFile.replace(classPath, ""));
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                if (Objects.nonNull(password)) {
                    reader = new PemReader(inputStreamReader);
                } else {
                    reader = new PemReader(inputStreamReader);
                }
            } else {
                Path path = Paths.get(caCrtFile);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(Files.readAllBytes(path));
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                if (Objects.nonNull(password)) {
                    reader = new PemReader(inputStreamReader);
                } else {
                    reader = new PemReader(inputStreamReader);
                }
            }
            return (T) reader.readPemObject();
        } finally {
            if (Objects.nonNull(reader)) {
                reader.close();
            }
        }
    }
}
