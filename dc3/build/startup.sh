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
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#

export LANG=zh_CN.gbk

app_path=$(cd $(dirname $0);pwd);

if [ ! -d logs ]; then
	mkdir -p logs/run/pid
fi

sh ${app_path}/shutdown.sh

java_args=" -XX:+UseConcMarkSweepGC -verbose:gc -Xloggc:./logs/run/gc.log -XX:+PrintGCDetails -XX:+PrintGCTimeStamps"

for file in ${app_path}/*.jar
do
    if test -f ${file}
    then
        jar=${file##*/}
        run_log="${app_path}/logs/run/${jar%-*}.run.log"
        nohup java ${java_args} -jar ${app_path}/${jar} >>${run_log} 2>&1 &

        echo "$!">${app_path}/logs/run/pid/${jar%-*}.pid
        echo "START at $DATE PID $PID">>${run_log}
        echo "[*PID $!] ${jar%-*} service startup"
    fi
done

cd $app_path
cd ../webpage
npm run dev

echo "$!">${app_path}/logs/run/pid/webpage.pid
echo "[*PID $!] webpage service startup"


