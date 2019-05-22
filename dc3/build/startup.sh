#!/bin/sh

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


