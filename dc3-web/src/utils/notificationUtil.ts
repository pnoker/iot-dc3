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
