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
import io.github.pnoker.common.exception.ConnectorException;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * X.509 certificate loading and SSL socket factory creation.
 * <p>
 * Loads PEM-encoded CA and client certificates via {@link CertificateFactory} and PEM-encoded
 * client private keys (PKCS#1 / PKCS#8, encrypted or plain) via BouncyCastle's PEM parser, then
 * assembles a mutual-TLS {@link SSLSocketFactory}.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
public class X509Util {

    private X509Util() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Create SSL socket factory with custom certificates
     *
     * @param caCrtFile CA certificate file path
     * @param crtFile   Client certificate file path
     * @param keyFile   Client private key file path
     * @param password  Private key password
     * @return Configured SSL socket factory
     */
    public static SSLSocketFactory getSSLSocketFactory(final String caCrtFile, final String crtFile,
                                                       final String keyFile, final String password) {
        try {
            Security.addProvider(new BouncyCastleProvider());

            // load CA certificate
            X509Certificate caCert = loadCertificate(caCrtFile);
            // CA certificate is used to authenticate server
            KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            caKeyStore.load(null, null);
            caKeyStore.setCertificateEntry("cacertfile", caCert);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(caKeyStore);

            // load client certificate
            X509Certificate cert = loadCertificate(crtFile);
            // load client private key
            PrivateKey key = loadPrivateKey(keyFile, password);
            // client key and certificates are sent to server, so it can authenticate us
            KeyStore certKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            certKeyStore.load(null, null);
            certKeyStore.setCertificateEntry("certfile", cert);
            certKeyStore.setKeyEntry("keyfile", key, password.toCharArray(),
                    new Certificate[]{cert});
            KeyManagerFactory keyManagerFactory = KeyManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(certKeyStore, password.toCharArray());

            // finally, create SSL socket factory
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

            return context.getSocketFactory();
        } catch (Exception e) {
            throw new ConnectorException("Failed to create X509 SSL socket factory: {}", e.getMessage(), e);
        }
    }

    /**
     * Load a PEM- or DER-encoded X.509 certificate from a classpath ({@code classpath:}) or filesystem path.
     */
    private static X509Certificate loadCertificate(String certFile) throws IOException, CertificateException {
        try (InputStream inputStream = open(certFile)) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certificateFactory.generateCertificate(inputStream);
        }
    }

    /**
     * Load a PEM-encoded private key, transparently handling PKCS#1 / PKCS#8 and encrypted variants.
     */
    private static PrivateKey loadPrivateKey(String keyFile, String password) throws IOException {
        char[] passwordChars = password == null ? new char[0] : password.toCharArray();
        try (Reader reader = new InputStreamReader(open(keyFile));
             PEMParser pemParser = new PEMParser(reader)) {
            Object pemObject = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
            return switch (pemObject) {
                // Encrypted traditional (PKCS#1) key, e.g. "Proc-Type: 4,ENCRYPTED"
                case PEMEncryptedKeyPair encryptedKeyPair -> {
                    PEMDecryptorProvider decryptor = new JcePEMDecryptorProviderBuilder().build(passwordChars);
                    yield converter.getKeyPair(encryptedKeyPair.decryptKeyPair(decryptor)).getPrivate();
                }
                // Encrypted PKCS#8 key ("ENCRYPTED PRIVATE KEY")
                case PKCS8EncryptedPrivateKeyInfo encryptedInfo -> {
                    try {
                        InputDecryptorProvider decryptor = new JceOpenSSLPKCS8DecryptorProviderBuilder()
                                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(passwordChars);
                        yield converter.getPrivateKey(encryptedInfo.decryptPrivateKeyInfo(decryptor));
                    } catch (OperatorCreationException | PKCSException e) {
                        throw new IOException("Failed to decrypt PKCS#8 private key", e);
                    }
                }
                // Plain traditional (PKCS#1) key
                case PEMKeyPair keyPair -> converter.getKeyPair(keyPair).getPrivate();
                // Plain PKCS#8 key ("PRIVATE KEY")
                case PrivateKeyInfo privateKeyInfo -> converter.getPrivateKey(privateKeyInfo);
                case null -> throw new IOException("No PEM private key found in: " + keyFile);
                default -> throw new IOException("Unsupported private key PEM content: " + pemObject.getClass().getName());
            };
        }
    }

    /**
     * Open a {@code classpath:}-prefixed resource or a filesystem path as an input stream.
     */
    private static InputStream open(String path) throws IOException {
        String classPath = "classpath:";
        if (path.startsWith(classPath)) {
            return X509Util.class.getResourceAsStream(path.replace(classPath, ""));
        }
        return new ByteArrayInputStream(Files.readAllBytes(Paths.get(path)));
    }

}
