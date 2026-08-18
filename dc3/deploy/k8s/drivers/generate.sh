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
  echo "generated driver-${driver}.yaml"
done < "$DIR/list.txt"
