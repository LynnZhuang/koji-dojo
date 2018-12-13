#!/bin/bash

set -x

echo "Create /opt/.pgpass"
cat <<EOF >> /opt/.pgpass
koji-db:5432:koji:koji:mypassword
EOF
chmod 600 /opt/.pgpass
ls -la /opt/

cat /opt/.pgpass

psql="PGPASSFILE=/opt/.pgpass psql --host=koji-db --username=koji koji"

cat /usr/local/src/koji/docs/schema.sql | $psql
echo "BEGIN WORK; INSERT INTO content_generator(name) VALUES('test-cg'); COMMIT WORK;" | $psql
