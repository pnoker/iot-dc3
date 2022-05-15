package com.dc3.common.sdk.utils;

import com.dc3.common.constant.Common;
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
            PEMReader reader;

            // load CA certificate
            X509Certificate caCert = loadCertificate(caCrtFile);
            // CA certificate is used to authenticate server
            KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            caKeyStore.load(null, null);
            caKeyStore.setCertificateEntry(Common.Driver.X509.CA_CERT_FILE, caCert);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(caKeyStore);

            // load client certificate
            X509Certificate cert = loadCertificate(crtFile);
            // load client private key
            KeyPair key = loadCertificate(keyFile, password);
            // client key and certificates are sent to server so it can authenticate us
            KeyStore certKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            certKeyStore.load(null, null);
            certKeyStore.setCertificateEntry(Common.Driver.X509.CERT_FILE, cert);
            certKeyStore.setKeyEntry(Common.Driver.X509.KEY_FILE, key.getPrivate(), password.toCharArray(), new java.security.cert.Certificate[]{cert});
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(certKeyStore, password.toCharArray());

            // finally, create SSL socket factory
            SSLContext context = SSLContext.getInstance(Common.Driver.X509.TLS_V1_2);
            context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

            return context.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private static <T> T loadCertificate(String caCrtFile) throws IOException {
        return loadCertificate(caCrtFile, null);
    }

    @SuppressWarnings("unchecked")
    private static <T> T loadCertificate(String caCrtFile, String password) throws IOException {
        T caCert = null;
        PEMReader reader = null;

        try {
            if (caCrtFile.startsWith(Common.Driver.X509.CLASS_PATH)) {
                reader = null != password ? new PEMReader(new InputStreamReader(X509Util.class.getResourceAsStream(caCrtFile.replace(Common.Driver.X509.CLASS_PATH, ""))), password::toCharArray)
                        : new PEMReader(new InputStreamReader(X509Util.class.getResourceAsStream(caCrtFile.replace(Common.Driver.X509.CLASS_PATH, ""))));
            } else {
                reader = null != password ? new PEMReader(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(caCrtFile)))), password::toCharArray)
                        : new PEMReader(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(caCrtFile)))));
            }
            caCert = (T) reader.readObject();
        } finally {
            if (null != reader) reader.close();
        }

        return caCert;
    }
}
