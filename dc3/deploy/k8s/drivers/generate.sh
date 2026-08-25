#!/usr/bin/env bash
#
# Regenerates drivers/driver-<name>.yaml from drivers/template.yaml and
# drivers/list.txt. The generated files are committed so `kubectl apply -k`
# works out of the box; re-run this script after adding a driver to list.txt.
#
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
while read -r driver; do
  [[ -z "$driver" || "$driver" == \#* ]] && continue
  sed "s/__DRIVER__/${driver}/g" "$DIR/template.yaml" > "$DIR/driver-${driver}.yaml"
  if [[ "$driver" == "listening-virtual" ]]; then
    # Inbound device sockets: expose the TCP/UDP listener ports (the NodePort
    # itself lives in service-listening-virtual.yaml).
    awk '/name: dc3-secrets/ { print; print "          ports:"; print "            - name: tcp"; print "              containerPort: 6270"; print "              protocol: TCP"; print "            - name: udp"; print "              containerPort: 6271"; print "              protocol: UDP"; next } { print }' \
      "$DIR/driver-${driver}.yaml" > "$DIR/driver-${driver}.yaml.tmp"
    mv "$DIR/driver-${driver}.yaml.tmp" "$DIR/driver-${driver}.yaml"
  fi
  echo "generated driver-${driver}.yaml"
done < "$DIR/list.txt"
