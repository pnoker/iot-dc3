/*
 * Copyright 2022 Pnoker All Rights Reserved
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

package io.github.pnoker.common.sdk.utils;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;

/**
 * @author pnoker
 */
@Slf4j
public class X509Util {

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
            // client key and certificates are sent to server so it can authenticate us
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
            throw new RuntimeException(e.getMessage());
        }
    }

    private static <T> T loadCertificate(String caCrtFile) throws IOException {
        return loadCertificateWithPassword(caCrtFile, null);
    }

    @SuppressWarnings("unchecked")
    private static <T> T loadCertificateWithPassword(String caCrtFile, String password) throws IOException {
        PEMReader reader = null;
        try {
            String classPath = "classpath:";
            if (caCrtFile.startsWith(classPath)) {
                reader = null != password ? new PEMReader(new InputStreamReader(X509Util.class.getResourceAsStream(caCrtFile.replace(classPath, ""))), password::toCharArray)
                        : new PEMReader(new InputStreamReader(X509Util.class.getResourceAsStream(caCrtFile.replace(classPath, ""))));
            } else {
                reader = null != password ? new PEMReader(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(caCrtFile)))), password::toCharArray)
                        : new PEMReader(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(caCrtFile)))));
            }
            return (T) reader.readObject();
        } finally {
            if (null != reader) reader.close();
        }
    }
}
