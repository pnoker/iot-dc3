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
 * Common entity types used throughout the application
 */

/**
 * Login information interface
 */
export interface Login {
  /** Tenant identifier */
  tenant?: string;
  /** Username */
  name?: string;
  /** Salt value for password encryption */
  salt?: string;
  /** Encrypted password */
  password?: string;
  /** Authentication token */
  token?: string;
}

/**
 * Attribute information interface
 */
export interface Attribute {
  /** Attribute ID */
  id: string;
  /** Attribute display name */
  name: string;
  /** Attribute internal name */
  attributeName: string;
}

/**
 * Dictionary interface for hierarchical data structures
 */
export interface Dictionary {
  /** Dictionary type/category */
  type: string;
  /** Display label */
  label: string;
  /** Internal value */
  value: string;
  /** Whether the item is disabled */
  disabled: boolean;
  /** Whether the item is expanded */
  expand: boolean;
  /** Child dictionary items */
  children: Array<Dictionary>;
}

/**
 * Sort order interface for table sorting
 */
export interface Order {
  /** Column name to sort by */
  column: string;
  /** Sort direction (true for ascending, false for descending) */
  asc: boolean;
}
