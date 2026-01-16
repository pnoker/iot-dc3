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
 * Generate random number with specified length
 *
 * @param len Number length
 * @param date Whether to append timestamp
 * @returns {string} Random number string
 */
export const randomLenNum = (len: number, date?: boolean): string => {
  const length = len ? len : 4;
  let random = '';

  // Generate random digits to ensure we always get the requested length
  for (let i = 0; i < length; i++) {
    random += Math.floor(Math.random() * 10).toString();
  }

  if (date) random = random + Date.now();
  return random;
};
