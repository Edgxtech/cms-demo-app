#!/bin/bash

echo "Configuring cmsdemo2 database, only needed for the demo.."

# Create the second organisation database, only required for demonstration purposes
# Normally two organisations wouldn't use the same postgres instance
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-EOSQL
    CREATE DATABASE cmsdemo2;
    GRANT ALL PRIVILEGES ON DATABASE cmsdemo2 TO "$POSTGRES_USER";
    \c cmsdemo2
    CREATE SCHEMA IF NOT EXISTS keycloak;
    CREATE SCHEMA IF NOT EXISTS lob_follower_service;
    CREATE SCHEMA IF NOT EXISTS public;
EOSQL