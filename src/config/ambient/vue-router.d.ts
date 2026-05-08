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

// Augment vue-router's RouteMeta so route definitions under src/config/router/**
// can use `meta: { title, icon }` with full type-checking.
import 'vue-router';

declare module 'vue-router' {
  interface RouteMeta {
    /** Page title used in document.title and menu labels */
    title?: string;
    /** Global Element Plus icon name (e.g. 'Promotion', 'List') used in the nav menu */
    icon?: string;
  }
}

export {};
