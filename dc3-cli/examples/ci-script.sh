#!/bin/bash
# CI/CD Integration Script for dc3-cli
# Uses environment variable for password (never commit real credentials)

set -e

# Check required env vars
if [ -z "$DC3_GATEWAY" ]; then
  echo "Error: DC3_GATEWAY is not set"
  exit 1
fi

if [ -z "$DC3_PASSWORD" ]; then
  echo "Error: DC3_PASSWORD is not set"
  exit 1
fi

# Configure and login
dc3 config set gateway "$DC3_GATEWAY"
dc3 auth login \
  --tenant "${DC3_TENANT:-default}" \
  --username "${DC3_USERNAME:-cicd-bot}" \
  --password "$DC3_PASSWORD" \
  --store env

echo "✓ Authenticated"

# Run commands
echo "=== Device List ==="
dc3 device list --format json

echo "=== Dashboard Health ==="
dc3 dashboard health --format json

echo "=== Alert Stats ==="
dc3 alert stats --format json

# Cleanup (optional — token will auto-expire)
echo "✓ CI checks completed"
