#!/bin/sh

#
#  Copyright 2019 Pnoker. All Rights Reserved.
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

app_path=$(
  cd $(dirname $0)
  pwd
)
project_name=iot-dc3
git_url=https://github.com/pnoker/iot-dc3.git

echo "[*] app path $app_path"
echo "[*] project $project_name"
echo "[*] git url $git_url"

cd $app_path
cd ../

git pull origin master
mvn clean -U package
cd webpage
npm install

echo "[*] init success"
