#!/bin/bash

set -x

echo "Create /opt/koji-clients/.pgpass"
cat <<EOF >> /opt/koji-clients/.pgpass
koji-db:5432:koji:koji:mypassword
EOF
chmod 600 /opt/koji-clients/.pgpass
ls -la /opt/koji-clients

cat /opt/koji-clients/.pgpass

export PGPASSFILE=/opt/koji-clients/.pgpass
psql="psql --host=koji-db --username=koji koji"

cat /usr/local/src/koji/docs/schema.sql | $psql
echo "BEGIN WORK; INSERT INTO content_generator(name) VALUES('test-cg'); COMMIT WORK;" | $psql
