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
 * Toggle gray mode
 *
 * @param status Whether to enable gray mode
 */
export const toggleGrayMode = (status: any) => {
  if (status) {
    document.body.className = document.body.className + ' grayMode';
  } else {
    document.body.className = document.body.className.replace(' grayMode', '');
  }
};

/**
 * Set theme
 *
 * @param name Theme name
 */
export const setTheme = (name: any) => {
  document.body.className = name;
};

/**
 * Dynamically load CSS
 *
 * @param url CSS file URL
 */
export const loadStyle = (url: string): void => {
  const link = document.createElement('link');
  link.type = 'text/css';
  link.rel = 'stylesheet';
  link.href = url;
  const head = document.getElementsByTagName('head')[0];
  if (head) {
    head.appendChild(link);
  }
};

/**
 * Listen for fullscreen changes
 *
 * @param callback Callback function
 */
export const listenFullscreen = (callback: () => void): void => {
  function listen() {
    callback();
  }

  document.addEventListener('fullscreenchange', function () {
    listen();
  });
  document.addEventListener('mozfullscreenchange', function () {
    listen();
  });
  document.addEventListener('webkitfullscreenchange', function () {
    listen();
  });
  document.addEventListener('msfullscreenchange', function () {
    listen();
  });
};
