#!/bin/sh

set -e

mongod --smallfiles --bind_ip_all &

while true; do
  mongo /dc3-mongo/config/iot-dc3.sql && break
  sleep 5
done

wait