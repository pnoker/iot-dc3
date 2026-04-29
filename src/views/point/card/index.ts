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

import { computed, defineComponent } from 'vue';
import {
  Bottom,
  CircleCheck,
  CircleClose,
  Edit,
  List,
  Location,
  Sunset,
  SwitchButton,
  Top,
} from '@element-plus/icons-vue';
import { useI18n } from 'vue-i18n';

import router from '@/config/router';

import { successMessage } from '@/utils/NotificationUtil';
import { copy } from '@/utils/CommonUtil';
import { timestamp } from '@/utils/DateUtil';

export default defineComponent({
  name: 'PointCard',
  components: {
    Edit,
    Sunset,
    List,
    Location,
    Bottom,
    Top,
  },
  props: {
    embedded: {
      type: Boolean,
      default: () => {
        return false;
      },
    },
    data: {
      type: Object,
      default: () => {
        return {
          pointName: '',
          enable: '',
          remark: '',
          createTime: '',
          operateTime: '',
        };
      },
    },
    profile: {
      type: Object,
      default: () => {
        return {};
      },
    },
    icon: {
      type: String,
      default: 'images/common/point.png',
    },
  },
  emits: ['disable-thing', 'enable-thing', 'delete-thing'],
  setup(props, { emit }) {
    const { t } = useI18n();

    // 图标
    const Icon = {
      SwitchButton,
      CircleCheck,
      CircleClose,
    };

    const pointTypeFlag = computed(() => {
      const pointTypeFlag = props.data.pointTypeFlag;
      if (pointTypeFlag === 'STRING') {
        return t('dataType.string');
      } else if (pointTypeFlag === 'BYTE') {
        return t('dataType.byte');
      } else if (pointTypeFlag === 'SHORT') {
        return t('dataType.short');
      } else if (pointTypeFlag === 'INT') {
        return t('dataType.int');
      } else if (pointTypeFlag === 'LONG') {
        return t('dataType.long');
      } else if (pointTypeFlag === 'FLOAT') {
        return t('dataType.float');
      } else if (pointTypeFlag === 'DOUBLE') {
        return t('dataType.double');
      } else if (pointTypeFlag === 'BOOLEAN') {
        return t('dataType.boolean');
      }
      return t('status.unknown');
    });

    const rwFlag = computed(() => {
      const rwFlag = props.data.rwFlag;
      if (rwFlag === 'R') {
        return t('status.readOnly');
      } else if (rwFlag === 'W') {
        return t('status.writeOnly');
      } else if (rwFlag === 'RW') {
        return t('status.readWrite');
      }
      return t('status.unknown');
    });

    const disableThing = () => {
      emit('disable-thing', props.data.id, props.data.profileId, () => {
        successMessage();
      });
    };

    const enableThing = () => {
      emit('enable-thing', props.data.id, props.data.profileId, () => {
        successMessage();
      });
    };

    const deleteThing = () => {
      emit('delete-thing', props.data.id, () => {
        successMessage();
      });
    };

    const edit = () => {
      router
        .push({
          name: 'pointEdit',
          query: { id: props.data.id, profileId: props.data.profileId, active: '0' },
        })
        .catch(() => {
          // nothing to do
        });
    };

    const detail = () => {
      router.push({ name: 'pointDetail', query: { id: props.data.id, active: 'detail' } }).catch(() => {
        // nothing to do
      });
    };

    return {
      pointTypeFlag,
      rwFlag,
      disableThing,
      enableThing,
      deleteThing,
      edit,
      detail,
      copyId: copy,
      timestamp,
      ...Icon,
    };
  },
});
