#!/bin/bash

# Flag to determine if organisation tables should be truncated (default: false)
truncate_organisation=false

# Parse command-line options
while getopts "o" opt; do
    case $opt in
        o)
            truncate_organisation=true
            ;;
        \?)
            echo "Usage: $0 [-o]"
            echo "  -o: Truncate organisation and organisation_currency tables"
            exit 1
            ;;
    esac
done

# Array of database connection details
# Format: "host,port,db_name,user,password"
databases=(
    "localhost,5433,cmsdemo,cmsuser,cmspass"
    "localhost,5433,cmsdemo2,cmsuser,cmspass"
)

# Function to truncate tables for a single database
truncate_tables() {
    local db_host="$1"
    local db_port="$2"
    local db_name="$3"
    local db_user="$4"
    local db_password="$5"

    # Export password to avoid prompt
    export PGPASSWORD="$db_password"

    echo "Truncating tables in database: $db_name"

    # Base truncate commands
    truncate_commands="
    TRUNCATE TABLE blockchain_publisher_consignment;
    TRUNCATE TABLE lob_follower_service.blockchain_reader_consignment;
    "

    # Add organisation tables if the flag is set
    if [ "$truncate_organisation" = true ]; then
        truncate_commands="
        TRUNCATE TABLE organisation CASCADE;
        TRUNCATE TABLE organisation_currency CASCADE;
        $truncate_commands"
    fi

    # Execute truncate commands
    psql -h "$db_host" -p "$db_port" -U "$db_user" -d "$db_name" -c "$truncate_commands"

    # Check if the command was successful
    if [ $? -eq 0 ]; then
        echo "Tables in $db_name truncated successfully."
    else
        echo "Error truncating tables in $db_name."
        exit 1
    fi

    # Unset the password variable for security
    unset PGPASSWORD
}

# Loop through the databases array
for db in "${databases[@]}"; do
    # Split the database string into components
    IFS=',' read -r db_host db_port db_name db_user db_password <<< "$db"
    truncate_tables "$db_host" "$db_port" "$db_name" "$db_user" "$db_password"
done

echo "All specified tables truncated successfully."