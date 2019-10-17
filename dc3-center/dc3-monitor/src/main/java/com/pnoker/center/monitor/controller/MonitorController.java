/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pnoker.center.monitor.controller;

import de.codecentric.boot.admin.server.ui.extensions.UiExtension;
import de.codecentric.boot.admin.server.ui.web.UiController;
import de.codecentric.boot.admin.server.web.AdminController;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * <p>
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@AdminController
public class MonitorController extends UiController {
    private final String publicUrl;

    public MonitorController(String publicUrl, String title, String brand, String favicon, String faviconDanger, List<UiExtension> uiExtensions, boolean notificationFilterEnabled, String publicUrl1) {
        super(publicUrl, title, brand, favicon, faviconDanger, uiExtensions, notificationFilterEnabled);
        this.publicUrl = publicUrl1;
    }

    @Override
    @ModelAttribute(value = "baseUrl", binding = false)
    public String getBaseUrl(UriComponentsBuilder uriBuilder) {
        UriComponents publicComponents = UriComponentsBuilder.fromUriString(publicUrl).build();
        if (publicComponents.getScheme() != null) {
            uriBuilder.scheme(publicComponents.getScheme());
        }
        if (publicComponents.getHost() != null) {
            uriBuilder.host(publicComponents.getHost());
        }
        if (publicComponents.getPort() != -1) {
            uriBuilder.port(publicComponents.getPort());
        }
        if (publicComponents.getPath() != null) {
            uriBuilder.path(publicComponents.getPath());
        }
        return uriBuilder.path("/").toUriString();
    }
}
