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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KeyLoaderTest {

    @Test
    void loadCreatesMissingDirectoryAndReusableKeystore(@TempDir Path temporaryDirectory) throws Exception {
        Path keystoreDirectory = temporaryDirectory.resolve("nested/opc-ua");

        KeyLoader generated = new KeyLoader().load(keystoreDirectory);
        Path keystore = keystoreDirectory.resolve("dc3-opc-ua-client.pfx");
        long size = Files.size(keystore);
        KeyLoader reloaded = new KeyLoader().load(keystoreDirectory);

        assertThat(keystoreDirectory).isDirectory();
        assertThat(keystore).isRegularFile();
        assertThat(size).isPositive();
        assertThat(generated.getClientCertificate()).isNotNull();
        assertThat(generated.getClientKeyPair()).isNotNull();
        assertThat(reloaded.getClientCertificate()).isNotNull();
        assertThat(reloaded.getClientKeyPair()).isNotNull();
        assertThat(Files.size(keystore)).isEqualTo(size);
    }
}
