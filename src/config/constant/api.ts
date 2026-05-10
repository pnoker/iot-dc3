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

/**
 * Gateway-prefixed base paths for every `src/api/*.ts` module.
 * The gateway (dc3-gateway) strips the `/api/v3` prefix and routes the
 * remainder to the matching center service (auth / data / manager).
 *
 * Keep these in sync with:
 *   - dc3-gateway/src/main/resources/application-*.yml (StripPrefix=2)
 *   - dc3-common-constant/.../service/{Auth,Data,Manager}Constant.java
 *     (SERVICE_NAME + URL_PREFIX fragments)
 */
export const API_AUTH_BASE = 'api/v3/auth';
export const API_AGENTIC_BASE = 'api/v3/agentic';
export const API_DATA_BASE = 'api/v3/data';
export const API_MANAGER_BASE = 'api/v3/manager';
