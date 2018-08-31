#!/bin/sh

TRY=60;

app_path=$(cd $(dirname $0);pwd);

for file in ${app_path}/*.jar
do
    if test -f ${file}
    then
        jar=${file##*/}

        PFILE=${app_path}/logs/run/pid/${jar%-*}.pid;

        if ! test -e ${PFILE}
        then
	        echo "no ${jar%-*}.pid file"
	        continue;
        fi

        PID=`cat ${app_path}/logs/run/pid/${jar%-*}.pid`;

        c=`ps -p ${PID}|wc -l`

        if test $c -le 1
        then
            echo "no process of PID $PID"
            continue;
        fi

        kill ${PID};

        printf "waiting pid:${PID} exit"

        for(( i=0;i<${TRY};i++ ))
        do
            c=`ps -p ${PID}|wc -l`
            echo $c
            if test $c -le 1
            then
                echo "";
                    echo "pid:${PID} exit.";
                continue;
            fi
            sleep 1;
            printf ".";

            i=$((i+1));
        done

        c=`ps -p ${PID}|wc -l`

        if test $c -gt 1
        then
            kill -9 ${PID};
            echo "pid:${PID} killed.";
        fi
    fi
done