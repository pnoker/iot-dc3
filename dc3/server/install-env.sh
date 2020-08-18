#!/bin/bash

#
#  Copyright 2018-2020 Pnoker. All Rights Reserved.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

set -e

# install certbot
mkdir /usr/local/certbot
cd /usr/local/certbot
wget https://dl.eff.org/certbot-auto
chmod a+x ./certbot-auto
./certbot-auto --install-only

# install openjdk 1.8
yum install java-1.8.0-openjdk

# install maven
yum install maven

# install docker
yum remove docker docker-client docker-client-latest docker-common docker-latest docker-latest-logrotate docker-logrotate docker-engine
yum install -y yum-utils gcc+ gcc-c++
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
yum install docker-ce docker-ce-cli containerd.io
systemctl enable docker
systemctl start docker

# install pip & docker compose
easy_install pip
pip install --upgrade pip
pip install -i http://pypi.douban.com/simple/ docker-compose

# install nodejs 12 & npm & yarn & cnpm
curl -sL https://rpm.nodesource.com/setup_12.x | sudo bash -
curl -sL https://dl.yarnpkg.com/rpm/yarn.repo | sudo tee /etc/yum.repos.d/yarn.repo
yum install yarn
yarn config set registry https://registry.npm.taobao.org --global
yarn config set disturl https://npm.taobao.org/dist --global

# install cnpm
npm install -g cnpm --registry=https://registry.npm.taobao.org

