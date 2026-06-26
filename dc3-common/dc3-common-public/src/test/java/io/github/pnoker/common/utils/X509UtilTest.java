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

import io.github.pnoker.common.exception.ConnectorException;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptor;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.SSLSocketFactory;
import javax.security.auth.x500.X500Principal;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class X509UtilTest {

    private static final String KEY_PASSWORD = "changeit";

    @BeforeAll
    static void registerProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static X509Certificate selfSignedCertificate(KeyPair keyPair) throws Exception {
        X500Principal subject = new X500Principal("CN=dc3-test");
        Instant now = Instant.now();
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                subject, BigInteger.ONE,
                Date.from(now.minus(1, ChronoUnit.DAYS)),
                Date.from(now.plus(365, ChronoUnit.DAYS)),
                subject, keyPair.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        return new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(builder.build(signer));
    }

    private static Path writeCertificate(Path file, X509Certificate cert) throws Exception {
        try (Writer writer = Files.newBufferedWriter(file);
             JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(cert);
        }
        return file;
    }

    private static Path writeEncryptedKey(Path file, KeyPair keyPair, String password) throws Exception {
        PEMEncryptor encryptor = new JcePEMEncryptorBuilder("DES-EDE3-CBC")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(password.toCharArray());
        try (Writer writer = Files.newBufferedWriter(file);
             JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(keyPair.getPrivate(), encryptor);
        }
        return file;
    }

    @Test
    void getSSLSocketFactoryLoadsCertificatesAndEncryptedKey(@TempDir Path dir) throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        X509Certificate cert = selfSignedCertificate(keyPair);
        Path caFile = writeCertificate(dir.resolve("ca.crt"), cert);
        Path certFile = writeCertificate(dir.resolve("client.crt"), cert);
        Path keyFile = writeEncryptedKey(dir.resolve("client.key"), keyPair, KEY_PASSWORD);

        SSLSocketFactory factory = X509Util.getSSLSocketFactory(
                caFile.toString(), certFile.toString(), keyFile.toString(), KEY_PASSWORD);

        assertThat(factory).isNotNull();
    }

    @Test
    void getSSLSocketFactoryWithWrongKeyPasswordFails(@TempDir Path dir) throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        X509Certificate cert = selfSignedCertificate(keyPair);
        Path caFile = writeCertificate(dir.resolve("ca.crt"), cert);
        Path certFile = writeCertificate(dir.resolve("client.crt"), cert);
        Path keyFile = writeEncryptedKey(dir.resolve("client.key"), keyPair, KEY_PASSWORD);

        assertThatThrownBy(() -> X509Util.getSSLSocketFactory(
                caFile.toString(), certFile.toString(), keyFile.toString(), "wrong-password"))
                .isInstanceOf(ConnectorException.class);
    }

}
