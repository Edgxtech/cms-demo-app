#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# Function to print status
print_status() {
  if [ $1 -eq 0 ]; then
    echo -e "${GREEN}✓ $2${NC}"
  else
    echo -e "${RED}✗ $2${NC}"
    exit 1
  fi
}

echo "=== Docker Compose Stats Snapshot ==="
docker compose stats --no-stream
echo ""

echo "=== Health Checks ==="

# Postgres: Check if accepting connections
echo "Checking Postgres..."
docker exec cms-demo-app-postgres-1 pg_isready -U "${DATABASE_USERNAME:-cmsuser}" -d "${DATABASE_NAME:-cmsdemo}" -p 5433 -h localhost
print_status $? "Postgres is ready"

# Ogmios: Check networkSynchronization == 1 or 1.00000
CNODE_CHECK_URL=http://localhost:${OGMIOS_PORT:-1337}/health
echo "Checking Cardano Node via Ogmios ($CNODE_CHECK_URL) ..."
OGMIOS_RESPONSE=$(curl -s -f $CNODE_CHECK_URL)
if [ $? -eq 0 ]; then
  NETWORK_SYNC=$(echo "$OGMIOS_RESPONSE" | jq -r '.networkSynchronization')
  if [ "$NETWORK_SYNC" = "1" ] || [ "$NETWORK_SYNC" = "1.00000" ]; then
    print_status 0 "Cardano Node is synchronised"
  else
    print_status 1 "Cardano Node is not fully synchronised (networkSynchronization: $NETWORK_SYNC)"
  fi
else
  print_status 1 "Cardano Node health check failed"
fi

# Indexer: Check synced == true
INDEXER_CHECK_URL=http://localhost:${INDEXER_PORT:-9090}/api/v1/tip
echo "Checking Indexer ($INDEXER_CHECK_URL) ..."
INDEXER_RESPONSE=$(curl -s -f $INDEXER_CHECK_URL)
if [ $? -eq 0 ]; then
  SYNCED=$(echo "$INDEXER_RESPONSE" | jq -r '.synced')
  if [ "$SYNCED" = "true" ]; then
    print_status 0 "Indexer is synced"
  else
    print_status 1 "Indexer is not synced (synced: $SYNCED)"
  fi
else
  print_status 1 "Indexer health check failed"
fi

# Keycloak: Check if server is responsive
KEYCLOAK_CHECK_URL=http://localhost:${KEYCLOAK_ADMIN_PORT:-9000}/health/ready
echo "Checking Keycloak ($KEYCLOAK_CHECK_URL) ..."
KEYCLOAK_RESPONSE=$(curl -s -f $KEYCLOAK_CHECK_URL)
if [ $? -eq 0 ]; then
  STATUS=$(echo "$KEYCLOAK_RESPONSE" | jq -r '.status')
  if [ "$STATUS" = "UP" ]; then
    print_status 0 "Keycloak is ready"
  else
    print_status 1 "Keycloak is not ready (status: $STATUS)"
  fi
else
  print_status 1 "Keycloak health check failed"
fi

# CMS Demo App: Check /api/hello endpoint
CMS_CHECK_URL=http://localhost:${CMS_APP_PORT:-8088}/api/hello
echo "Checking CMS Demo App ($CMS_CHECK_URL) ..."
CMS_RESPONSE=$(curl -s -f $CMS_CHECK_URL)
if [ $? -eq 0 ]; then
  if [ "$CMS_RESPONSE" = "Hello" ]; then
    print_status 0 "CMS Demo App is healthy"
  else
    print_status 1 "CMS Demo App returned unexpected response: $CMS_RESPONSE"
  fi
else
  print_status 1 "CMS Demo App health check failed"
fi

echo -e "${GREEN}All checks passed successfully!${NC}"