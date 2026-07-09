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

import {ElNotification} from 'element-plus';
import i18n from '@/config/i18n';
import {logger} from './log';
import {isNull} from './validationUtil';

/**
 * Show success notification
 *
 * @param message Notification message content
 * @param title Notification title
 */
export const successMessage = (message?: string, title: string = i18n.global.t('common.notification.successTitle')) => {
  if (isNull(message)) {
    message = i18n.global.t('common.notification.successDefault');
  }

  ElNotification({
    type: 'success',
    title: title,
    dangerouslyUseHTMLString: true,
    message: message,
  });
};

/**
 * Show warning notification
 *
 * @param message Notification message content
 * @param title Notification title
 * @param error Error object for debugging
 */
export const warnMessage = (
  message?: string,
  title = i18n.global.t('common.notification.warningTitle'),
  error?: unknown
) => {
  if (isNull(message)) {
    message = i18n.global.t('common.notification.warningDefault');
  }

  if (error) {
    logger.debug(error);
  }

  ElNotification({
    type: 'warning',
    title: title,
    dangerouslyUseHTMLString: true,
    message: message,
  });
};

/**
 * Show error notification
 *
 * @param message Notification message content
 * @param title Notification title
 * @param error Error object for debugging
 */
export const failMessage = (
  message?: string,
  title = i18n.global.t('common.notification.errorTitle'),
  error?: unknown
) => {
  if (isNull(message)) {
    message = i18n.global.t('common.notification.errorDefault');
  }

  if (error) {
    logger.debug(error);
  }

  ElNotification({
    type: 'error',
    title: title,
    dangerouslyUseHTMLString: true,
    message: message,
  });
};
