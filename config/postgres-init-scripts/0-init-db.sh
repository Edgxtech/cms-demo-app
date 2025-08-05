#!/bin/bash
set -e

# Ensure POSTGRES_DB is set
if [ -z "$POSTGRES_DB" ]; then
  echo "ERROR: POSTGRES_DB environment variable is not set"
  exit 1
fi

echo "Configuring cmsdemo database..."

# Create SQL commands using the POSTGRES_DB environment variable
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-EOSQL
    GRANT ALL PRIVILEGES ON DATABASE "$POSTGRES_DB" TO "$POSTGRES_USER";
    \c "$POSTGRES_DB"
    CREATE SCHEMA IF NOT EXISTS keycloak;
    CREATE SCHEMA IF NOT EXISTS lob_follower_service;
    CREATE SCHEMA IF NOT EXISTS public;
EOSQL