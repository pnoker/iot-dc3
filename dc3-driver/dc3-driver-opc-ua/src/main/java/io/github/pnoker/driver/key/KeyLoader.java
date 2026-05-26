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

package io.github.pnoker.driver.key;

import io.github.pnoker.common.utils.HostUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateBuilder;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateGenerator;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.regex.Pattern;

/**
 * Certificate and key loader for OPC-UA client authentication. This class handles loading
 * and generation of X.509 certificates and key pairs for secure OPC-UA communication. It
 * creates self-signed certificates if they don't exist and manages the PKCS12 keystore
 * for the client.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
public class KeyLoader {

    /**
     * IPv4 address regex pattern, used to validate if a string is a legal IPv4 address.
     */
    private static final Pattern IP_ADDR_PATTERN = Pattern
            .compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    /**
     * PKCS12 keystore password, used to load/generate client certificate keystore.
     * <p>
     * Override the default via the {@code OPCUA_KEYSTORE_PASSWORD} environment variable
     * or the {@code dc3.opcua.keystore-password} system property.
     */
    private static final char[] PASSWORD = loadKeystorePassword();

    private static char[] loadKeystorePassword() {
        String password = System.getenv("OPCUA_KEYSTORE_PASSWORD");
        if (password == null || password.isBlank()) {
            password = System.getProperty("dc3.opcua.keystore-password", "password");
        }
        return password.toCharArray();
    }

    /**
     * Client certificate alias, used to read client private key and certificate from
     * keystore.
     */
    private static final String CLIENT_ALIAS = "client-ai";

    @Getter
    private X509Certificate clientCertificate;

    @Getter
    private KeyPair clientKeyPair;

    /**
     * Loads or creates the client certificate and key pair.
     * <p>
     * If the keystore doesn't exist, a new self-signed certificate is generated with the
     * configured subject alternative names (hostnames and IP addresses). The certificate
     * and private key are stored in a PKCS12 keystore.
     * </p>
     *
     * @param baseDir the base directory where the keystore file is located
     * @return this KeyLoader instance with loaded certificate and key pair
     * @throws Exception if certificate generation or loading fails
     */
    public KeyLoader load(Path baseDir) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        Path serverKeyStore = baseDir.resolve("dc3-opc-ua-client.pfx");

        if (!Files.exists(serverKeyStore)) {
            log.info("OPC UA client keystore generating, path={}", serverKeyStore);
            keyStore.load(null, PASSWORD);
            KeyPair keyPair = SelfSignedCertificateGenerator.generateRsaKeyPair(2048);
            SelfSignedCertificateBuilder builder = new SelfSignedCertificateBuilder(keyPair)
                    .setCommonName("DC3 Opc Ua Client")
                    .setOrganization("dc3")
                    .setOrganizationalUnit("iot")
                    .setLocalityName("BeiJing")
                    .setStateName("BJ")
                    .setCountryCode("ZN")
                    .setApplicationUri("urn:dc3:opc:ua:client")
                    .addDnsName("localhost")
                    .addIpAddress("127.0.0.1");

            // Get as many hostnames and IP addresses as we can listed in the certificate.
            for (String hostname : HostUtil.getHostNames("0.0.0.0")) {
                if (IP_ADDR_PATTERN.matcher(hostname).matches()) {
                    builder.addIpAddress(hostname);
                } else {
                    builder.addDnsName(hostname);
                }
            }

            X509Certificate certificate = builder.build();
            keyStore.setKeyEntry(CLIENT_ALIAS, keyPair.getPrivate(), PASSWORD, new X509Certificate[]{certificate});
            try (OutputStream out = Files.newOutputStream(serverKeyStore)) {
                keyStore.store(out, PASSWORD);
            }
            log.info("OPC UA client keystore generated, path={}", serverKeyStore);
        } else {
            try (InputStream in = Files.newInputStream(serverKeyStore)) {
                keyStore.load(in, PASSWORD);
            }
            log.debug("OPC UA client keystore loaded, path={}", serverKeyStore);
        }

        Key serverPrivateKey = keyStore.getKey(CLIENT_ALIAS, PASSWORD);
        if (serverPrivateKey instanceof PrivateKey) {
            clientCertificate = (X509Certificate) keyStore.getCertificate(CLIENT_ALIAS);
            PublicKey serverPublicKey = clientCertificate.getPublicKey();
            clientKeyPair = new KeyPair(serverPublicKey, (PrivateKey) serverPrivateKey);
            log.debug("OPC UA client certificate loaded, alias={}", CLIENT_ALIAS);
        }

        return this;
    }

}
