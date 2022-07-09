<!--
  - Copyright (c) 2022. Pnoker. All Rights Reserved.
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -     http://www.apache.org/licenses/LICENSE-2.0
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
	<div class="things-card things-card-application">
		<el-card shadow="hover">
			<div class="things-card-content">
				<div class="things-card__body">
					<div class="things-card-body-content">
						<div class="things-card-body-content-application-icon" @click="show()">
							<el-avatar shape="square" :size="120" fit="fill" :src="icon"></el-avatar>
						</div>
					</div>
				</div>
				<div class="things-card__footer">
					<div class="things-card-footer-title">
						<span class="things-card-body-content-application-name">{{ name }}</span>
					</div>
				</div>
			</div>
		</el-card>
	</div>
</template>

<script>
import { encode } from 'js-base64'

export default {
	name: 'ApplicationCard',
	props: {
		name: {
			type: String,
			default: '数据看板',
		},
		icon: {
			type: String,
			default: 'images/app/application.png',
		},
		url: {
			type: String,
			default: '',
		},
	},
	methods: {
		show() {
			if (this.url) {
				this.$router.push({ name: 'application', query: { id: encode(this.url) } }).catch(() => {
					// nothing to do
				})
			}
		},
	},
}
</script>

<style lang="less">
@import '~@/components/card/styles/things-card.less';

.things-card-application {
	width: 140px;
	height: 170px;
	margin-right: 10px;
	margin-bottom: 10px;

	.el-card {
		width: 100%;
		min-width: 140px !important;
		min-height: 170px !important;
		box-sizing: border-box;
	}
}

.things-card-body-content-application-icon {
	width: 120px;
	cursor: pointer;

	.el-avatar {
		background: transparent;
	}
}

.things-card-body-content-application-name {
	display: block;
	width: 120px;
	overflow: hidden;
	white-space: nowrap;
	text-overflow: ellipsis;
}
</style>
