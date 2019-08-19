#!/bin/sh

set -e

mongod --smallfiles --bind_ip_all &

while true; do
  mongo /dc3-mongo/config/init_mongo.js && break
  sleep 5
done

wait