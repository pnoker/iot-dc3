#!/usr/bin/env bash
#
# Copyright 2016-present the IoT DC3 original author or authors.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

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
