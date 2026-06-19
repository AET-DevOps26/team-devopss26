#!/bin/bash
set -e
set -u

function create_database() {
    local database=$1
    echo "  Creating database: '$database'"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<EOSQL
CREATE DATABASE "$database";
GRANT ALL PRIVILEGES ON DATABASE "$database" TO "$POSTGRES_USER";
EOSQL
}

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then   
  IFS=',' read -ra DBS <<< "$POSTGRES_MULTIPLE_DATABASES"
  echo "Multiple database creation requested: $POSTGRES_MULTIPLE_DATABASES"
  for db in "${DBS[@]}"; do
      create_database "$db"
  done
  echo "All databases created successfully!"                                                                                                                           
fi                                         